package com.zcamstudio.kawaiipb.feature.flow.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.zcamstudio.kawaiipb.domain.model.KioskFlowStage
import com.zcamstudio.kawaiipb.domain.model.LayoutBrandingArea
import com.zcamstudio.kawaiipb.domain.model.LayoutSafeArea
import com.zcamstudio.kawaiipb.domain.model.StripLayout
import com.zcamstudio.kawaiipb.domain.model.StripSize
import com.zcamstudio.kawaiipb.domain.model.TemplatePhotoSlot
import org.json.JSONArray
import org.json.JSONObject

private const val AssetImageCacheSize = 16
private const val CapturePhotoCacheSize = 24

private val assetImageCache = object : LruCache<String, ImageBitmap>(AssetImageCacheSize) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = 1
}

private val capturePhotoCache = object : LruCache<String, ImageBitmap>(CapturePhotoCacheSize) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = 1
}

fun stageDuration(stage: KioskFlowStage, settings: FlowTimerSettings = FlowTimerSettings()): Int = when (stage) {
    KioskFlowStage.CameraMode -> settings.cameraModeDuration
    KioskFlowStage.Capture -> settings.captureDuration
    KioskFlowStage.PhotoAssignment -> settings.photoAssignmentDuration
    KioskFlowStage.StripSize -> settings.stripSizeDuration
    KioskFlowStage.Preview -> settings.previewDuration
    KioskFlowStage.Printing -> 0
    KioskFlowStage.Qr -> settings.qrCodeDuration
}

fun stripSizePreviewAssetPath(size: StripSize): String = when (size) {
    StripSize.TwoByFour -> "layouts/strip_layout/2x4.png"
    StripSize.TwoByThree -> "layouts/strip_layout/2x3.png"
    StripSize.TwoByTwo -> "layouts/strip_layout/2x2.png"
}

fun stripSizeLayoutAssetPath(size: StripSize): String = when (size) {
    StripSize.TwoByFour -> "layouts/strip_2x4.json"
    StripSize.TwoByThree -> "layouts/strip_2x3.json"
    StripSize.TwoByTwo -> "layouts/strip_2x2.json"
}

fun flowStageTitle(stage: KioskFlowStage): String = when (stage) {
    KioskFlowStage.CameraMode -> "Choose Camera"
    KioskFlowStage.Capture -> "Capture Session"
    KioskFlowStage.PhotoAssignment -> "Photo Assignment"
    KioskFlowStage.StripSize -> "Strip Size"
    KioskFlowStage.Preview -> "Preview"
    KioskFlowStage.Printing -> "Printing"
    KioskFlowStage.Qr -> "QR Code"
}

fun flowStageSubtitle(stage: KioskFlowStage): String = when (stage) {
    KioskFlowStage.CameraMode -> "カメラを選んでください"
    KioskFlowStage.Capture -> "Eight automatic shots power the photo strip."
    KioskFlowStage.PhotoAssignment -> "Assign your captured images"
    KioskFlowStage.StripSize -> "Choose the strip layout."
    KioskFlowStage.Preview -> "Check the final composition."
    KioskFlowStage.Printing -> "Save the rendered strip as a PDF."
    KioskFlowStage.Qr -> "Scan to download before the session expires."
}

fun advanceFlowStateForTick(
    state: FlowUiState,
    newSessionSeconds: Int,
    nextPrintProgress: Float,
    nextQrExpiry: Int
): FlowUiState {
    val nextState = state.copy(
        sessionSecondsLeft = newSessionSeconds,
        printProgress = nextPrintProgress,
        qrExpirySeconds = nextQrExpiry,
        captureShotCountdown = if (state.stage == KioskFlowStage.Capture && state.isCaptureCountdownActive && state.capturedFrames.size < 8 && !state.isCaptureInProgress) {
            if (state.captureShotCountdown > 1) state.captureShotCountdown - 1 else 0
        } else state.captureShotCountdown,
        isCaptureCountdownActive = state.isCaptureCountdownActive && state.captureShotCountdown > 1
    )

    val stageSecondsLeft = (nextState.stageSecondsLeft - 1).coerceAtLeast(0)
    val advancedState = nextState.copy(stageSecondsLeft = stageSecondsLeft)

    return if (stageSecondsLeft == 0) {
        when (advancedState.stage) {
            KioskFlowStage.CameraMode -> advancedState.copy(stage = KioskFlowStage.Capture, stageSecondsLeft = stageDuration(KioskFlowStage.Capture, advancedState.flowTimerSettings), captureShotCountdown = 0, isCaptureCountdownActive = false, summaryMessage = "Capture session ready")
            KioskFlowStage.Capture -> advancedState.copy(stage = KioskFlowStage.StripSize, stageSecondsLeft = stageDuration(KioskFlowStage.StripSize, advancedState.flowTimerSettings), captureShotCountdown = 0, isCaptureCountdownActive = false, isCaptureInProgress = false, summaryMessage = "Time expired, choose strip size")
            KioskFlowStage.PhotoAssignment -> {
                val autoFilled = autoFillRemainingFrames(advancedState)
                autoFilled.copy(stage = KioskFlowStage.Preview, stageSecondsLeft = stageDuration(KioskFlowStage.Preview, advancedState.flowTimerSettings), summaryMessage = if (autoFilled.photoAssignmentAssignments.any { it == null }) "Time expired, review the strip" else "Time expired, auto-filled empty frames and review the strip")
            }
            KioskFlowStage.StripSize -> advancedState.copy(stage = KioskFlowStage.PhotoAssignment, stageSecondsLeft = stageDuration(KioskFlowStage.PhotoAssignment, advancedState.flowTimerSettings), summaryMessage = "Time expired, assign photos")
            KioskFlowStage.Preview -> advancedState.copy(stage = KioskFlowStage.Preview, stageSecondsLeft = 0, summaryMessage = "Review the final strip")
            KioskFlowStage.Printing -> advancedState.copy(stage = KioskFlowStage.Printing, stageSecondsLeft = 0, printStatus = advancedState.printStatus, summaryMessage = advancedState.summaryMessage)
            KioskFlowStage.Qr -> advancedState
        }
    } else {
        advancedState
    }
}

private fun autoFillRemainingFrames(state: FlowUiState): FlowUiState {
    val availablePhotos = state.capturedFrames.indices.toList()
    if (availablePhotos.isEmpty()) return state

    val alreadyAssigned = state.photoAssignmentAssignments.filterNotNull().toSet()
    val unassignedPhotos = availablePhotos.filterNot { it in alreadyAssigned }.shuffled()
    if (unassignedPhotos.isEmpty()) return state

    val assignmentBuilder = state.photoAssignmentAssignments.toMutableList()
    var photoCursor = 0
    assignmentBuilder.forEachIndexed { idx, assigned ->
        if (assigned == null && photoCursor < unassignedPhotos.size) {
            assignmentBuilder[idx] = unassignedPhotos[photoCursor]
            photoCursor += 1
        }
    }

    return state.copy(
        photoAssignmentAssignments = assignmentBuilder,
        photoAssignmentTransforms = List(state.photoAssignmentAssignments.size) { PhotoTransform(PhotoAssignmentInitialScale) },
        photoAssignmentSelectedSlot = null,
        photoAssignmentShowCapturedList = false
    )
}

fun loadStripLayoutFromAssets(context: Context, assetPath: String): StripLayout? {
    return try {
        context.assets.open(assetPath).use { stream ->
            val json = stream.bufferedReader().use { it.readText() }
            parseStripLayout(JSONObject(json))
        }
    } catch (ex: Exception) {
        null
    }
}

fun parseStripLayout(json: JSONObject): StripLayout {
    val stripType = json.optString("layoutId", json.optString("layoutType", "2x4"))
    val paper = json.optJSONObject("paper")
    val canvasWidth = paper?.optInt("width", 1200) ?: 1200
    val canvasHeight = paper?.optInt("height", 1800) ?: 1800
    val backgroundImage = json.optString("backgroundImage", "")
    val output = json.optJSONObject("output")
    val safeAreaJson = json.optJSONObject("safeArea")
    val brandingAreaJson = json.optJSONObject("brandingArea")
    val slots = mutableListOf<TemplatePhotoSlot>()
    val arr = json.optJSONArray("slots") ?: JSONArray()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val slot = TemplatePhotoSlot(
            id = o.optInt("id", i + 1),
            strip = o.optInt("strip", 1),
            x = o.optInt("x", 0),
            y = o.optInt("y", 0),
            width = o.optInt("width", 0),
            height = o.optInt("height", 0),
            rotation = o.optDouble("rotation", 0.0).toFloat(),
            mask = null,
            visible = o.optBoolean("visible", true)
        )
        slots.add(slot)
    }
    return StripLayout(
        stripType = stripType,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        photoSlots = slots,
        backgroundImage = backgroundImage,
        doubleStrip = output?.optBoolean("doubleStrip", false) ?: false,
        safeArea = safeAreaJson?.let {
            LayoutSafeArea(
                left = it.optInt("left", 0),
                top = it.optInt("top", 0),
                right = it.optInt("right", 0),
                bottom = it.optInt("bottom", 0)
            )
        },
        brandingArea = brandingAreaJson?.let {
            LayoutBrandingArea(
                enabled = it.optBoolean("enabled", false),
                height = it.optInt("height", 0)
            )
        }
    )
}

fun resolveLayoutAssetPath(layout: StripLayout?, fallbackAssetPath: String = "layouts/strip_2x4_base.png"): String? {
    return layout?.backgroundImage?.takeIf { it.isNotBlank() }
        ?: fallbackAssetPath.takeIf { it.isNotBlank() }
}


fun loadAssetImage(context: Context, assetPath: String): ImageBitmap? {
    if (assetPath.isBlank()) return null
    assetImageCache.get(assetPath)?.let { return it }

    return try {
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()?.also { assetImageCache.put(assetPath, it) }
        }
    } catch (ex: Exception) {
        null
    }
}

// Template overlay functions removed

fun loadCapturePhoto(imagePath: String?): ImageBitmap? {
    if (imagePath == null) return null
    capturePhotoCache.get(imagePath)?.let { return it }

    return try {
        val bitmap = BitmapFactory.decodeFile(imagePath) ?: return null

        val rotation = runCatching {
            // Handle EXIF orientation so previews appear correctly rotated
            val exif = ExifInterface(imagePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)

        if (rotation == 0f) {
            bitmap.asImageBitmap().also { capturePhotoCache.put(imagePath, it) }
        } else {
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).asImageBitmap()
                .also { capturePhotoCache.put(imagePath, it) }
        }
    } catch (ex: Exception) {
        runCatching {
            BitmapFactory.decodeFile(imagePath)?.asImageBitmap()?.also { capturePhotoCache.put(imagePath, it) }
        }.getOrNull()
    }
}
