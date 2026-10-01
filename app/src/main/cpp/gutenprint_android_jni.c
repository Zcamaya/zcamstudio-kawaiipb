#include <jni.h>
#include <android/log.h>
#include <gutenprint/gutenprint.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#define GUTENPRINT_LOG_TAG "KawaiiGutenprintUSB"
#define GUTENPRINT_LOGI(...) __android_log_print(ANDROID_LOG_INFO, GUTENPRINT_LOG_TAG, __VA_ARGS__)
#define GUTENPRINT_LOGE(...) __android_log_print(ANDROID_LOG_ERROR, GUTENPRINT_LOG_TAG, __VA_ARGS__)
#define USB_CHUNK_SIZE 4096
#define USB_TRANSFER_TIMEOUT_MS 60000

enum {
    GUTENPRINT_STAGE_VALIDATE = 1,
    GUTENPRINT_STAGE_INITIALIZE,
    GUTENPRINT_STAGE_FIND_L120,
    GUTENPRINT_STAGE_CONFIGURE,
    GUTENPRINT_STAGE_START_JOB,
    GUTENPRINT_STAGE_PRINT_IMAGE,
    GUTENPRINT_STAGE_END_JOB,
    GUTENPRINT_STAGE_COMPLETE
};

typedef struct {
    JNIEnv *env;
    jobject connection;
    jobject endpoint;
    jmethodID bulkTransfer;
    int failed;
    jint lastTransfer;
    int64_t bytesWritten;
} UsbOutput;

typedef struct {
    const uint8_t *pixels;
    int width;
    int height;
    UsbOutput *output;
} RgbImage;

static int gutenprintInitialized;

static void writeDiagnostics(JNIEnv *env, jlongArray diagnostics, jint stage, const UsbOutput *output) {
    if (diagnostics == NULL || (*env)->GetArrayLength(env, diagnostics) < 3) return;
    const jlong values[] = {
        stage,
        output == NULL ? 0 : (jlong)output->bytesWritten,
        output == NULL ? 0 : output->lastTransfer
    };
    (*env)->SetLongArrayRegion(env, diagnostics, 0, 3, values);
}

static void writeUsbBytes(void *data, const char *buffer, size_t length) {
    UsbOutput *output = (UsbOutput *)data;
    if (output == NULL || output->failed || length == 0) return;

    size_t offset = 0;
    while (offset < length) {
        size_t chunkLength = length - offset;
        if (chunkLength > USB_CHUNK_SIZE) chunkLength = USB_CHUNK_SIZE;

        jbyteArray chunk = (*output->env)->NewByteArray(output->env, (jsize)chunkLength);
        if (chunk == NULL) {
            output->failed = 1;
            output->lastTransfer = -2;
            return;
        }
        (*output->env)->SetByteArrayRegion(
            output->env,
            chunk,
            0,
            (jsize)chunkLength,
            (const jbyte *)(buffer + offset)
        );
        if ((*output->env)->ExceptionCheck(output->env)) {
            (*output->env)->ExceptionClear(output->env);
            (*output->env)->DeleteLocalRef(output->env, chunk);
            output->failed = 1;
            output->lastTransfer = -2;
            return;
        }

        jint transferred = (*output->env)->CallIntMethod(
            output->env,
            output->connection,
            output->bulkTransfer,
            output->endpoint,
            chunk,
            (jint)chunkLength,
            USB_TRANSFER_TIMEOUT_MS
        );
        (*output->env)->DeleteLocalRef(output->env, chunk);
        if ((*output->env)->ExceptionCheck(output->env)) {
            (*output->env)->ExceptionClear(output->env);
            output->failed = 1;
            output->lastTransfer = -2;
            return;
        }
        output->lastTransfer = transferred;
        if (transferred <= 0) {
            output->failed = 1;
            return;
        }
        offset += (size_t)transferred;
        output->bytesWritten += transferred;
    }
}

static void reportGutenprintError(void *data, const char *message, size_t length) {
    (void)data;
    if (message == NULL || length == 0) return;
    const size_t safeLength = length > 800 ? 800 : length;
    __android_log_print(ANDROID_LOG_ERROR, GUTENPRINT_LOG_TAG, "%.*s", (int)safeLength, message);
}

static void imageInitialize(stp_image_t *image) {
    (void)image;
}

static void imageReset(stp_image_t *image) {
    (void)image;
}

static int imageWidth(stp_image_t *image) {
    const RgbImage *rgb = (const RgbImage *)image->rep;
    return rgb->width;
}

static int imageHeight(stp_image_t *image) {
    const RgbImage *rgb = (const RgbImage *)image->rep;
    return rgb->height;
}

static stp_image_status_t imageGetRow(stp_image_t *image, unsigned char *data, size_t byteLimit, int row) {
    RgbImage *rgb = (RgbImage *)image->rep;
    if (rgb->output->failed || row < 0 || row >= rgb->height || byteLimit < (size_t)rgb->width * 3) {
        return STP_IMAGE_STATUS_ABORT;
    }
    memcpy(data, rgb->pixels + (size_t)row * rgb->width * 3, (size_t)rgb->width * 3);
    return STP_IMAGE_STATUS_OK;
}

static const char *imageAppName(stp_image_t *image) {
    (void)image;
    return "KawaiiPB";
}

static void imageConclude(stp_image_t *image) {
    (void)image;
}

static int initializeGutenprint(const char *dataPath) {
    setenv("STP_DATA_PATH", dataPath, 1);
    if (!gutenprintInitialized) {
        if (stp_init() != 0) return 0;
        gutenprintInitialized = 1;
    }
    return 1;
}

JNIEXPORT jint JNICALL
Java_com_zcamstudio_kawaiipb_feature_printing_GutenprintNative_printL120(
    JNIEnv *env,
    jobject receiver,
    jobject connection,
    jobject endpoint,
    jstring dataDirectory,
    jint pageWidthMils,
    jint pageHeightMils,
    jint width,
    jint height,
    jbyteArray rgbBytes,
    jlongArray diagnostics
) {
    (void)receiver;
    jint stage = GUTENPRINT_STAGE_VALIDATE;
    UsbOutput output = {0};
    stp_vars_t *vars = NULL;
    const char *dataPath = NULL;
    jbyte *pixels = NULL;
    int jobStarted = 0;
    int result = -1;
    RgbImage rgb = {0};
    stp_image_t image = {0};

    if (connection == NULL || endpoint == NULL || dataDirectory == NULL || rgbBytes == NULL ||
        width <= 0 || height <= 0 || pageWidthMils <= 0 || pageHeightMils <= 0) {
        writeDiagnostics(env, diagnostics, stage, &output);
        return -1;
    }

    const int64_t requiredBytes = (int64_t)width * height * 3;
    if (requiredBytes > (*env)->GetArrayLength(env, rgbBytes) || requiredBytes > INT32_MAX) {
        writeDiagnostics(env, diagnostics, stage, &output);
        return -1;
    }

    dataPath = (*env)->GetStringUTFChars(env, dataDirectory, NULL);
    if (dataPath == NULL) {
        writeDiagnostics(env, diagnostics, stage, &output);
        return -1;
    }

    output.env = env;
    output.connection = connection;
    output.endpoint = endpoint;
    jclass connectionClass = (*env)->GetObjectClass(env, connection);
    output.bulkTransfer = (*env)->GetMethodID(
        env,
        connectionClass,
        "bulkTransfer",
        "(Landroid/hardware/usb/UsbEndpoint;[BII)I"
    );
    (*env)->DeleteLocalRef(env, connectionClass);
    if (output.bulkTransfer == NULL) goto cleanup;

    stage = GUTENPRINT_STAGE_INITIALIZE;
    if (!initializeGutenprint(dataPath)) goto cleanup;

    stage = GUTENPRINT_STAGE_FIND_L120;
    const stp_printer_t *printer = stp_get_printer_by_driver("escp2-l120");
    if (printer == NULL) goto cleanup;

    vars = stp_vars_create();
    if (vars == NULL) goto cleanup;
    stp_set_printer_defaults(vars, printer);
    stp_set_errfunc(vars, reportGutenprintError);
    stp_set_errdata(vars, &output);
    const char *paperSize = NULL;
    if (pageWidthMils == 6000 && pageHeightMils == 4000) paperSize = "w288h432";
    else if (pageWidthMils == 11693 && pageHeightMils == 8268) paperSize = "A4";
    else if (pageWidthMils == 11000 && pageHeightMils == 8500) paperSize = "Letter";
    else if (pageWidthMils == 14000 && pageHeightMils == 8500) paperSize = "Legal";
    if (paperSize == NULL) goto cleanup;
    stp_set_string_parameter(vars, "PageSize", paperSize);
    stp_set_string_parameter(vars, "Orientation", width > height ? "Landscape" : "Portrait");
    stp_set_outfunc(vars, writeUsbBytes);
    stp_set_outdata(vars, &output);
    stp_set_errdata(vars, &output);

    pixels = (*env)->GetByteArrayElements(env, rgbBytes, NULL);
    if (pixels == NULL) goto cleanup;
    rgb = (RgbImage){(const uint8_t *)pixels, width, height, &output};
    image.init = imageInitialize;
    image.reset = imageReset;
    image.width = imageWidth;
    image.height = imageHeight;
    image.get_row = imageGetRow;
    image.get_appname = imageAppName;
    image.conclude = imageConclude;
    image.rep = &rgb;

    stage = GUTENPRINT_STAGE_START_JOB;
    if (!stp_start_job(vars, &image)) goto cleanup;
    jobStarted = 1;

    stage = GUTENPRINT_STAGE_PRINT_IMAGE;
    result = stp_print(vars, &image);
    if (output.failed || result != 1) {
        result = -1;
        goto cleanup;
    }

    stage = GUTENPRINT_STAGE_END_JOB;
    if (!stp_end_job(vars, &image) || output.failed) {
        result = -1;
        goto cleanup;
    }
    jobStarted = 0;
    stage = GUTENPRINT_STAGE_COMPLETE;
    result = 0;

cleanup:
    if (jobStarted && vars != NULL) stp_end_job(vars, &image);
    if (pixels != NULL) (*env)->ReleaseByteArrayElements(env, rgbBytes, pixels, JNI_ABORT);
    if (vars != NULL) stp_vars_destroy(vars);
    if (dataPath != NULL) (*env)->ReleaseStringUTFChars(env, dataDirectory, dataPath);
    writeDiagnostics(env, diagnostics, stage, &output);
    GUTENPRINT_LOGI(
        "L120 Gutenprint result=%d stage=%d bytes=%lld lastTransfer=%d media=%dx%d mils",
        result,
        stage,
        (long long)output.bytesWritten,
        output.lastTransfer,
        pageWidthMils,
        pageHeightMils
    );
    return result;
}
