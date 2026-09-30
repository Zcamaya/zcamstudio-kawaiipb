package com.zcamstudio.kawaiipb.feature.flow.presentation

import com.zcamstudio.kawaiipb.domain.model.KioskFlowStage
import com.zcamstudio.kawaiipb.domain.model.StripSize
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowPresentationHelpersTest {

    @Test
    fun stageDurationUsesExpectedDurations() {
        assertEquals(20, stageDuration(KioskFlowStage.CameraMode))
        assertEquals(90, stageDuration(KioskFlowStage.Capture))
        assertEquals(30, stageDuration(KioskFlowStage.Qr))
    }

    @Test
    fun stageTextHelpersReturnExpectedLabels() {
        assertEquals("Choose Camera", flowStageTitle(KioskFlowStage.CameraMode))
        assertEquals("Preview", flowStageTitle(KioskFlowStage.Preview))
        assertEquals("Check the final composition.", flowStageSubtitle(KioskFlowStage.Preview))
    }

    @Test
    fun photoPanUsesSlotRelativeOffsetsAcrossPreviewAndPrintSizes() {
        val normalizedX = normalizePhotoOffset(50f, 200f)
        val normalizedY = normalizePhotoOffset(-25f, 100f)

        assertEquals(0.25f, normalizedX, 0.001f)
        assertEquals(-0.25f, normalizedY, 0.001f)
        assertEquals(100f, photoOffsetPixels(normalizedX, 400f), 0.001f)
        assertEquals(-50f, photoOffsetPixels(normalizedY, 200f), 0.001f)
        assertEquals(0f, normalizePhotoOffset(50f, 0f), 0f)
    }

    @Test
    fun photoContainScaleKeepsTheWholeImageInsideItsSlot() {
        val scale = photoContainScale(slotWidthPx = 100f, slotHeightPx = 150f, imageWidthPx = 1600f, imageHeightPx = 900f)

        assertEquals(0.0625f, scale, 0.0001f)
        assertEquals(100f, 1600f * scale, 0.01f)
        assertEquals(56.25f, 900f * scale, 0.01f)
        assertEquals(0f, photoContainScale(0f, 150f, 1600f, 900f), 0f)
    }

    @Test
    fun timeoutTransitionAdvancesToTheNextStage() {
        val state = FlowUiState(
            stage = KioskFlowStage.CameraMode,
            stageSecondsLeft = 1,
            sessionSecondsLeft = 60,
            captureShotCountdown = 3,
            isCaptureCountdownActive = true
        )

        val advanced = advanceFlowStateForTick(
            state = state,
            newSessionSeconds = 59,
            nextPrintProgress = 0f,
            nextQrExpiry = 300
        )

        assertEquals(KioskFlowStage.Capture, advanced.stage)
        assertEquals(stageDuration(KioskFlowStage.Capture), advanced.stageSecondsLeft)
        assertEquals("Capture session ready", advanced.summaryMessage)
    }

    @Test
    fun captureTimeoutWithoutPhotosStaysOnCaptureWithNoPhotoMessage() {
        val emptyCaptureState = FlowUiState(
            stage = KioskFlowStage.Capture,
            stageSecondsLeft = 1,
            sessionSecondsLeft = 60
        )
        val captureWithPhotoState = emptyCaptureState.copy(
            capturedFrames = listOf(com.zcamstudio.kawaiipb.domain.model.CaptureFrame(1, 42, "Shot 1", "photo.jpg"))
        )

        val emptyResult = advanceFlowStateForTick(emptyCaptureState, 59, 0f, 300)
        val photoResult = advanceFlowStateForTick(captureWithPhotoState, 59, 0f, 300)

        assertEquals(KioskFlowStage.Capture, emptyResult.stage)
        assertEquals("No photo captured", emptyResult.cameraError)
        assertEquals("No photo captured", emptyResult.summaryMessage)
        assertEquals(KioskFlowStage.StripSize, photoResult.stage)
    }

    @Test
    fun stripSizePreviewAssetsUseLayoutPreviewImages() {
        assertEquals("layouts/strip_layout/2x4.png", stripSizePreviewAssetPath(StripSize.TwoByFour))
        assertEquals("layouts/strip_layout/2x3.png", stripSizePreviewAssetPath(StripSize.TwoByThree))
        assertEquals("layouts/strip_layout/2x2.png", stripSizePreviewAssetPath(StripSize.TwoByTwo))
        assertEquals("layouts/pb_card_uncut_2p_stack.png", stripSizePreviewAssetPath(StripSize.TwoByOneStack))
        assertEquals("layouts/pb_card_uncut_3p_left.png", stripSizePreviewAssetPath(StripSize.ThreeByOneLeft))
        assertEquals("layouts/pb_card_uncut_3p_right.png", stripSizePreviewAssetPath(StripSize.ThreeByOneRight))
        assertEquals("layouts/pb_split_horiz_2p_grid.png", stripSizePreviewAssetPath(StripSize.TwoByTwoGrid))
        assertEquals("layouts/pb_card_uncut_4p_banner.png", stripSizePreviewAssetPath(StripSize.FourByBanner))
    }

    @Test
    fun stickerBaseHeightTracksContainerHeight() {
        assertEquals(125f, stickerBaseHeightPx(1000f), 0.01f)
        assertEquals(187.5f, stickerBaseHeightPx(1000f, 1.5f), 0.01f)
        assertEquals(50f, stickerBaseHeightPx(400f), 0.01f)
    }
}
