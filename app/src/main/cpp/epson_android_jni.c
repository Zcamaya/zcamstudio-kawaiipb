#include <jni.h>
#include <android/log.h>
#include <stdint.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>

#include "epson-escpr-api.h"

#define PRINT_LOG_TAG "KawaiiEpsonUSB"
#define PRINT_LOGI(...) __android_log_print(ANDROID_LOG_INFO, PRINT_LOG_TAG, __VA_ARGS__)
#define PRINT_LOGE(...) __android_log_print(ANDROID_LOG_ERROR, PRINT_LOG_TAG, __VA_ARGS__)

static JavaVM *driverVm;
static jobject usbConnection;
static jobject inputEndpoint;
static jobject outputEndpoint;
static jmethodID bulkTransferMethod;
static jmethodID controlTransferMethod;
static EPS_PRINTER discoveredPrinter;
static EPS_BOOL printerDiscovered;
static int64_t totalBytesWritten;
static jint lastBulkTransferResult;
static jint printerInterfaceId;
static jint printerVendorId;
static jint printerProductId;
static char lastDeviceId[256];
static jint lastDeviceIDTransferResult;

#define USB_TRANSFER_CHUNK_SIZE 4096
#define USB_TRANSFER_TIMEOUT_MS 60000

enum {
    PRINT_STAGE_VALIDATE = 1,
    PRINT_STAGE_INIT_DRIVER,
    PRINT_STAGE_FIND_PRINTER,
    PRINT_STAGE_SET_PRINTER,
    PRINT_STAGE_START_JOB,
    PRINT_STAGE_START_PAGE,
    PRINT_STAGE_PRINT_BAND,
    PRINT_STAGE_END_PAGE,
    PRINT_STAGE_END_JOB,
    PRINT_STAGE_NO_USB_DATA,
    PRINT_STAGE_COMPLETE
};

static void writeDiagnostics(JNIEnv *env, jlongArray diagnostics, jint stage) {
    if (diagnostics == NULL || (*env)->GetArrayLength(env, diagnostics) < 4) return;
    const jlong values[] = {stage, (jlong)totalBytesWritten, lastBulkTransferResult, lastDeviceIDTransferResult};
    (*env)->SetLongArrayRegion(env, diagnostics, 0, 4, values);
}

static void writeDeviceId(JNIEnv *env, jbyteArray output) {
    if (output == NULL) return;
    jsize capacity = (*env)->GetArrayLength(env, output);
    jsize length = (jsize)strnlen(lastDeviceId, sizeof(lastDeviceId));
    if (length > capacity) length = capacity;
    if (length > 0) (*env)->SetByteArrayRegion(env, output, 0, length, (const jbyte *)lastDeviceId);
}

static JNIEnv *currentEnv(void) {
    JNIEnv *env = NULL;
    if ((*driverVm)->GetEnv(driverVm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        return NULL;
    }
    return env;
}

static EPS_FILEDSC openPortal(const EPS_USB_DEVICE *device) {
    (void)device;
    return usbConnection == NULL ? EPS_INVALID_FILEDSC : (EPS_FILEDSC)(intptr_t)1;
}

static EPS_INT32 closePortal(EPS_FILEDSC descriptor) {
    (void)descriptor;
    return 0;
}

static EPS_FILEDSC findFirst(EPS_USB_DEVICE *device) {
    if (device == NULL || usbConnection == NULL) return EPS_INVALID_FILEDSC;
    device->vid = (EPS_UINT32)printerVendorId;
    device->pid = (EPS_UINT32)printerProductId;
    device->port = (EPS_UINT32)printerInterfaceId;
    return (EPS_FILEDSC)(intptr_t)1;
}

static EPS_BOOL findNext(EPS_FILEDSC descriptor, EPS_USB_DEVICE *device) {
    (void)descriptor;
    (void)device;
    return FALSE;
}

static EPS_BOOL findClose(EPS_FILEDSC descriptor) {
    (void)descriptor;
    return TRUE;
}

static EPS_INT32 readPortal(EPS_FILEDSC descriptor, EPS_UINT8 *buffer, EPS_INT32 length, EPS_INT32 *readBytes) {
    (void)descriptor;
    JNIEnv *env = currentEnv();
    if (env == NULL || usbConnection == NULL || inputEndpoint == NULL || buffer == NULL || readBytes == NULL || length <= 0) {
        return -1;
    }

    jbyteArray chunk = (*env)->NewByteArray(env, length);
    if (chunk == NULL) return -1;
    jint result = (*env)->CallIntMethod(env, usbConnection, bulkTransferMethod, inputEndpoint, chunk, length, USB_TRANSFER_TIMEOUT_MS);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        (*env)->DeleteLocalRef(env, chunk);
        return -1;
    }
    if (result < 0) {
        (*env)->DeleteLocalRef(env, chunk);
        return -1;
    }
    if (result > 0) {
        (*env)->GetByteArrayRegion(env, chunk, 0, result, (jbyte *)buffer);
    }
    (*env)->DeleteLocalRef(env, chunk);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        return -1;
    }
    *readBytes = result;
    return 0;
}

static EPS_INT32 getDeviceID(EPS_FILEDSC descriptor, EPS_INT8 *deviceId, EPS_INT32 *size) {
    (void)descriptor;
    JNIEnv *env = currentEnv();
    if (env == NULL || usbConnection == NULL || controlTransferMethod == NULL || deviceId == NULL || size == NULL || *size <= 0) {
        return -1;
    }

    const EPS_INT32 capacity = *size;
    memset(lastDeviceId, 0, sizeof(lastDeviceId));
    lastDeviceIDTransferResult = -1;
    jbyteArray response = (*env)->NewByteArray(env, capacity + 2);
    if (response == NULL) return -1;
    jint result = (*env)->CallIntMethod(
        env,
        usbConnection,
        controlTransferMethod,
        0xA1,
        0,
        0,
        printerInterfaceId,
        response,
        capacity + 2,
        5000
    );
    lastDeviceIDTransferResult = result;
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        (*env)->DeleteLocalRef(env, response);
        return -1;
    }
    if (result < 2) {
        (*env)->DeleteLocalRef(env, response);
        return -1;
    }

    jbyte lengthBytes[2];
    (*env)->GetByteArrayRegion(env, response, 0, 2, lengthBytes);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        (*env)->DeleteLocalRef(env, response);
        return -1;
    }
    const EPS_INT32 declaredLength = (((EPS_UINT8)lengthBytes[0]) << 8) | (EPS_UINT8)lengthBytes[1];
    if (declaredLength < 2) {
        (*env)->DeleteLocalRef(env, response);
        return -1;
    }

    const EPS_INT32 deviceIdLength = declaredLength - 2;
    if (deviceIdLength > result - 2) {
        *size = deviceIdLength;
        (*env)->DeleteLocalRef(env, response);
        return -1;
    }
    const EPS_INT32 copyLength = deviceIdLength < capacity ? deviceIdLength : capacity;
    (*env)->GetByteArrayRegion(env, response, 2, copyLength, (jbyte *)deviceId);
    (*env)->DeleteLocalRef(env, response);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        return -1;
    }
    if (copyLength < capacity) deviceId[copyLength] = '\0';
    const EPS_INT32 storedLength = copyLength < (EPS_INT32)sizeof(lastDeviceId) - 1
        ? copyLength
        : (EPS_INT32)sizeof(lastDeviceId) - 1;
    memcpy(lastDeviceId, deviceId, (size_t)storedLength);
    lastDeviceId[storedLength] = '\0';
    *size = copyLength;
    return 0;
}

static EPS_INT32 softReset(EPS_FILEDSC descriptor) {
    (void)descriptor;
    JNIEnv *env = currentEnv();
    if (env == NULL || usbConnection == NULL || controlTransferMethod == NULL) return -1;
    jint result = (*env)->CallIntMethod(
        env,
        usbConnection,
        controlTransferMethod,
        0x21,
        2,
        0,
        printerInterfaceId,
        NULL,
        0,
        5000
    );
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        return -1;
    }
    return result < 0 ? -1 : 0;
}

static EPS_INT32 writePortal(
    EPS_FILEDSC descriptor,
    const EPS_UINT8 *buffer,
    EPS_INT32 length,
    EPS_INT32 *written
) {
    (void)descriptor;
    JNIEnv *env = currentEnv();
    if (env == NULL || usbConnection == NULL || outputEndpoint == NULL || written == NULL) {
        PRINT_LOGE("USB write missing JNI context, connection, endpoint, or byte count");
        return -1;
    }

    *written = 0;
    while (*written < length) {
        const EPS_INT32 chunkLength = (length - *written) > USB_TRANSFER_CHUNK_SIZE
            ? USB_TRANSFER_CHUNK_SIZE
            : (length - *written);
        jbyteArray chunk = (*env)->NewByteArray(env, chunkLength);
        if (chunk == NULL) return -1;
        (*env)->SetByteArrayRegion(env, chunk, 0, chunkLength, (const jbyte *)(buffer + *written));
        jint transferLength = (*env)->CallIntMethod(
            env,
            usbConnection,
            bulkTransferMethod,
            outputEndpoint,
            chunk,
            chunkLength,
            USB_TRANSFER_TIMEOUT_MS
        );
        (*env)->DeleteLocalRef(env, chunk);
        if ((*env)->ExceptionCheck(env)) {
            lastBulkTransferResult = -2;
            (*env)->ExceptionClear(env);
            PRINT_LOGE("USB bulk write threw after %d/%d bytes", *written, length);
            return -1;
        }
        lastBulkTransferResult = transferLength;
        if (transferLength <= 0) {
            PRINT_LOGE("USB bulk write stopped after %d/%d bytes (last transfer=%d)", *written, length, transferLength);
            return -1;
        }
        *written += transferLength;
        totalBytesWritten += transferLength;
    }
    return 0;
}

static EPS_INT32 findPrinter(EPS_PRINTER printer) {
    discoveredPrinter = printer;
    printerDiscovered = TRUE;
    return 0;
}

static void *allocateMemory(size_t size) {
    return malloc(size);
}

static void freeMemory(void *memory) {
    free(memory);
}

static EPS_INT32 sleepMilliseconds(EPS_UINT32 milliseconds) {
    usleep(milliseconds * 1000);
    return 0;
}

static EPS_UINT32 getTimeSeconds(void) {
    return (EPS_UINT32)time(NULL);
}

static jint printRgb(
    JNIEnv *env,
    jobject connection,
    jint interfaceId,
    jint vendorId,
    jint productId,
    jobject inEndpoint,
    jobject outEndpoint,
    jint mediaSizeId,
    jint width,
    jint height,
    jbyteArray rgb,
    jlongArray diagnostics,
    jbyteArray deviceIdOutput
) {
    jint failureStage = PRINT_STAGE_VALIDATE;
    if (connection == NULL || inEndpoint == NULL || outEndpoint == NULL || rgb == NULL || width <= 0 || height <= 0) {
        writeDiagnostics(env, diagnostics, failureStage);
        return -1;
    }

    EPS_ERR_CODE result = -1;
    const jsize rgbLength = (*env)->GetArrayLength(env, rgb);
    const int64_t requiredLength = (int64_t)width * height * 3;
    if (requiredLength > rgbLength || requiredLength > INT32_MAX) {
        writeDiagnostics(env, diagnostics, failureStage);
        return -1;
    }
    totalBytesWritten = 0;
    lastBulkTransferResult = 0;
    lastDeviceIDTransferResult = -1;
    memset(lastDeviceId, 0, sizeof(lastDeviceId));
    printerInterfaceId = interfaceId;
    printerVendorId = vendorId;
    printerProductId = productId;
    PRINT_LOGI("Starting ESC/P-R print: media=%d, raster=%dx%d, rgbBytes=%d", mediaSizeId, width, height, rgbLength);

    failureStage = PRINT_STAGE_INIT_DRIVER;
    usbConnection = (*env)->NewGlobalRef(env, connection);
    inputEndpoint = (*env)->NewGlobalRef(env, inEndpoint);
    outputEndpoint = (*env)->NewGlobalRef(env, outEndpoint);
    if (usbConnection == NULL || inputEndpoint == NULL || outputEndpoint == NULL) goto cleanup;

    jclass connectionClass = (*env)->GetObjectClass(env, connection);
    bulkTransferMethod = (*env)->GetMethodID(
        env,
        connectionClass,
        "bulkTransfer",
        "(Landroid/hardware/usb/UsbEndpoint;[BII)I"
    );
    controlTransferMethod = (*env)->GetMethodID(env, connectionClass, "controlTransfer", "(IIII[BII)I");
    (*env)->DeleteLocalRef(env, connectionClass);
    if (bulkTransferMethod == NULL || controlTransferMethod == NULL) {
        PRINT_LOGE("Unable to resolve UsbDeviceConnection USB transfer methods");
        goto cleanup;
    }

    EPS_USB_FUNC usbFunctions = {0};
    usbFunctions.version = EPS_USBFUNC_VER_CUR;
    usbFunctions.openPortal = openPortal;
    usbFunctions.closePortal = closePortal;
    usbFunctions.readPortal = readPortal;
    usbFunctions.writePortal = writePortal;
    usbFunctions.findFirst = findFirst;
    usbFunctions.findNext = findNext;
    usbFunctions.findClose = findClose;
    usbFunctions.getDeviceID = getDeviceID;
    usbFunctions.softReset = softReset;

    EPS_CMN_FUNC commonFunctions = {0};
    commonFunctions.version = EPS_CMNFUNC_VER_CUR;
    commonFunctions.findCallback = findPrinter;
    commonFunctions.memAlloc = allocateMemory;
    commonFunctions.memFree = freeMemory;
    commonFunctions.sleep = sleepMilliseconds;
    commonFunctions.getTime = getTimeSeconds;

    result = epsInitDriver(EPS_COMM_USB_BID, &usbFunctions, NULL, &commonFunctions);
    PRINT_LOGI("epsInitDriver returned %d", result);
    if (result != EPS_ERR_NONE) goto cleanup;

    failureStage = PRINT_STAGE_FIND_PRINTER;
    printerDiscovered = FALSE;
    result = epsFindPrinter(EPS_PROTOCOL_USB, 0);
    PRINT_LOGI("epsFindPrinter returned %d; printer discovered=%d", result, printerDiscovered);
    if (result != EPS_ERR_NONE || !printerDiscovered) goto release_driver;

    failureStage = PRINT_STAGE_SET_PRINTER;
    result = epsSetPrinter(&discoveredPrinter);
    PRINT_LOGI("epsSetPrinter returned %d", result);
    if (result != EPS_ERR_NONE) goto release_driver;

    EPS_JOB_ATTRIB job = {0};
    job.version = EPS_JOB_ATTRIB_VER_CUR;
    job.colorPlane = EPS_CP_FULLCOLOR;
    job.inputResolution = EPS_IR_300X300;
    job.mediaSizeIdx = mediaSizeId;
    job.mediaTypeIdx = EPS_MTID_PLAIN;
    job.printLayout = EPS_MLID_BORDERS;
    job.printQuality = EPS_MQID_NORMAL;
    job.printDirection = EPS_PD_BIDIREC;
    job.colorMode = EPS_CM_COLOR;
    job.copies = 1;
    job.pageNum = 1;

    failureStage = PRINT_STAGE_START_JOB;
    result = epsStartJob(&job);
    PRINT_LOGI("epsStartJob returned %d for media ID %d at %dx%d", result, mediaSizeId, width, height);
    if (result != EPS_ERR_NONE) goto release_driver;

    EPS_PAGE_ATTRIB page = {0};
    page.version = 0;
    failureStage = PRINT_STAGE_START_PAGE;
    result = epsStartPage(&page, 1);
    PRINT_LOGI("epsStartPage returned %d", result);
    if (result == EPS_ERR_NONE) {
        jbyte *pixels = (*env)->GetByteArrayElements(env, rgb, NULL);
        if (pixels == NULL) {
            result = -1;
        } else {
            jint lineOffset = 0;
            while (lineOffset < height && result == EPS_ERR_NONE) {
                failureStage = PRINT_STAGE_PRINT_BAND;
                EPS_UINT32 bandHeight = (EPS_UINT32)(height - lineOffset);
                if (bandHeight > 64) bandHeight = 64;
                result = epsPrintBand(
                    (const EPS_UINT8 *)pixels + ((int64_t)lineOffset * width * 3),
                    (EPS_UINT32)width,
                    &bandHeight
                );
                if (result == EPS_ERR_NONE && bandHeight == 0) result = -1;
                lineOffset += (jint)bandHeight;
            }
            PRINT_LOGI("Raster transfer stopped at row %d/%d with result %d", lineOffset, height, result);
            (*env)->ReleaseByteArrayElements(env, rgb, pixels, JNI_ABORT);
        }
        if (result == EPS_ERR_NONE) failureStage = PRINT_STAGE_END_PAGE;
        EPS_ERR_CODE endPageResult = epsEndPage(0);
        PRINT_LOGI("epsEndPage returned %d", endPageResult);
        if (result == EPS_ERR_NONE) result = endPageResult;
    }

    {
        if (result == EPS_ERR_NONE) failureStage = PRINT_STAGE_END_JOB;
        EPS_ERR_CODE endJobResult = epsEndJob();
        PRINT_LOGI("epsEndJob returned %d", endJobResult);
        if (result == EPS_ERR_NONE) result = endJobResult;
    }

release_driver:
    PRINT_LOGI("USB bytes transferred=%lld", (long long)totalBytesWritten);
    if (result == EPS_ERR_NONE && totalBytesWritten == 0) {
        PRINT_LOGE("ESC/P-R returned success without sending any USB bytes");
        result = EPS_ERR_COMM_ERROR;
        failureStage = PRINT_STAGE_NO_USB_DATA;
    }
    {
        EPS_ERR_CODE releaseResult = epsReleaseDriver();
        PRINT_LOGI("epsReleaseDriver returned %d; final result=%d", releaseResult, result);
    }
cleanup:
    if (result == EPS_ERR_NONE) failureStage = PRINT_STAGE_COMPLETE;
    writeDiagnostics(env, diagnostics, failureStage);
    writeDeviceId(env, deviceIdOutput);
    if (usbConnection != NULL) (*env)->DeleteGlobalRef(env, usbConnection);
    if (inputEndpoint != NULL) (*env)->DeleteGlobalRef(env, inputEndpoint);
    if (outputEndpoint != NULL) (*env)->DeleteGlobalRef(env, outputEndpoint);
    usbConnection = NULL;
    inputEndpoint = NULL;
    outputEndpoint = NULL;
    bulkTransferMethod = NULL;
    controlTransferMethod = NULL;
    return result;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void)reserved;
    driverVm = vm;
    return JNI_VERSION_1_6;
}

JNIEXPORT jint JNICALL
Java_com_zcamstudio_kawaiipb_feature_printing_EpsonEscprNative_printRgb(
    JNIEnv *env,
    jobject receiver,
    jobject connection,
    jint interfaceId,
    jint vendorId,
    jint productId,
    jobject inEndpoint,
    jobject outEndpoint,
    jint mediaSizeId,
    jint width,
    jint height,
    jbyteArray rgb,
    jlongArray diagnostics,
    jbyteArray deviceIdOutput
) {
    (void)receiver;
    return printRgb(
        env,
        connection,
        interfaceId,
        vendorId,
        productId,
        inEndpoint,
        outEndpoint,
        mediaSizeId,
        width,
        height,
        rgb,
        diagnostics,
        deviceIdOutput
    );
}