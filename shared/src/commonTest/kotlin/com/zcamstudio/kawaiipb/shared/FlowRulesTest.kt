package com.zcamstudio.kawaiipb.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlowRulesTest {
    @Test
    fun timerDurationMatchesStageAndNormalizesAdminValues() {
        val settings = FlowTimerSettings(cameraModeSeconds = 3, captureSeconds = 300, preCaptureDelaySeconds = 50)

        assertEquals(5, durationFor(SessionStage.CAMERA_MODE, settings.normalized()))
        assertEquals(240, durationFor(SessionStage.CAPTURE, settings.normalized()))
        assertEquals(10, settings.normalized().preCaptureDelaySeconds)
        assertEquals(0, FlowTimerSettings(preCaptureDelaySeconds = -5).normalized().preCaptureDelaySeconds)
        assertEquals(0, durationFor(SessionStage.PRINTING, settings))
    }

    @Test
    fun sessionReducerTracksCaptureSuccessAndFailure() {
        val started = reduceSessionState(SessionState(), SessionAction.StartSession("test-session"))
        val cameraReady = reduceSessionState(started, SessionAction.ContinueFromCameraMode)
        val capturing = reduceSessionState(cameraReady, SessionAction.StartCapture)
        val saved = reduceSessionState(capturing, SessionAction.CaptureSucceeded("photo.jpg"))
        val failed = reduceSessionState(capturing, SessionAction.CaptureFailed("camera busy"))

        assertEquals(SessionStage.CAMERA_MODE, started.stage)
        assertEquals("photo.jpg", saved.capturedPhotos.single())
        assertFalse(saved.isCaptureInProgress)
        assertTrue(failed.status.contains("camera busy"))
        assertFalse(failed.isCaptureInProgress)
        assertEquals("camera busy", failed.cameraError)
        assertFalse(failed.isCaptureCountdownActive)
        assertEquals(started, reduceSessionState(started, SessionAction.StartCapture))
    }

    @Test
    fun sessionReducerFollowsAndroidScreenStageSequence() {
        var state = SessionState()
        state = reduceSessionState(state, SessionAction.StartSession("session"))
        state = reduceSessionState(state, SessionAction.ContinueFromCameraMode)
        repeat(8) { index ->
            state = reduceSessionState(state, SessionAction.StartCapture)
            state = reduceSessionState(state, SessionAction.CaptureSucceeded("photo-$index.jpg"))
        }
        assertTrue(state.captureComplete)
        state = reduceSessionState(state, SessionAction.ContinueFromCaptureComplete)
        assertEquals(SessionStage.STRIP_SIZE, state.stage)
        state = reduceSessionState(state, SessionAction.ContinueFromStripSize)
        assertEquals(SessionStage.PHOTO_ASSIGNMENT, state.stage)
        state = reduceSessionState(state, SessionAction.ContinueFromPhotoAssignment)
        assertEquals(SessionStage.PREVIEW, state.stage)
        state = reduceSessionState(state, SessionAction.BeginPrinting)
        assertEquals(SessionStage.PRINTING, state.stage)
    }

    @Test
    fun photoAssignmentsValidateIndexesRemoveAndAutofillOnlyEmptyActiveSlots() {
        val initial = PhotoAssignmentState(4, listOf(null, 1, null, null))
        val assigned = assignPhoto(initial, photoIndex = 2, slotIndex = 0)
        val invalid = assignPhoto(assigned, photoIndex = 9, slotIndex = 2)
        val filled = autoFillPhotoAssignments(assigned, activeSlotCount = 3)
        val removed = removePhotoAssignment(filled, slotIndex = 1)

        assertEquals(listOf(2, 1, null, null), assigned.assignments)
        assertEquals(assigned, invalid)
        assertEquals(listOf(2, 1, 0, null), filled.assignments)
        assertEquals(listOf(2, null, 0, null), removed.assignments)
        assertEquals(initial, removePhotoAssignment(initial, slotIndex = 10))
    }

    @Test
    fun selectedAndFirstEmptySlotAssignmentMatchesAndroidRules() {
        val initial = PhotoAssignmentState(3, listOf(null, 1, null, null))

        assertEquals(listOf(2, 1, null, null), assignCapturedPhoto(initial, 2, selectedSlot = 0).assignments)
        assertEquals(initial, assignCapturedPhoto(initial, 2, selectedSlot = 1))
        assertEquals(listOf(2, 1, null, null), assignCapturedPhoto(initial, 2, activeSlotCount = 2).assignments)
        assertEquals(initial, assignCapturedPhoto(initial, 8))
    }

    @Test
    fun shuffleResetAndTransformUpdatesMatchAssignmentEditorRules() {
        val initial = PhotoAssignmentState(3, listOf(0, 1, 2, 7))
        val shuffled = shufflePhotoAssignments(initial, activeSlotCount = 2, shuffledPhotoOrder = listOf(2, 0, 1))

        assertEquals(listOf(2, 0, 2, 7), shuffled.assignments)
        assertEquals(listOf(null, null, null, null), resetPhotoAssignments(initial).assignments)
        assertEquals(0.5f, PhotoTransform(scale = 1.7f, offsetX = 12f).withScale(-1f).scale)
        assertEquals(12f, PhotoTransform(scale = 1.7f, offsetX = 12f).withScale(-1f).offsetX)
    }

    @Test
    fun flowTickCountsDownAndRequestsCaptureAtZero() {
        val ticking = FlowTickState(
            stage = SessionStage.CAPTURE,
            stageSecondsLeft = 15,
            sessionSecondsLeft = 100,
            capturedPhotoCount = 2,
            isCaptureCountdownActive = true,
            captureCountdown = 1
        )

        val result = advanceFlowTick(ticking)

        assertEquals(14, result.state.stageSecondsLeft)
        assertEquals(99, result.state.sessionSecondsLeft)
        assertEquals(0, result.state.captureCountdown)
        assertFalse(result.state.isCaptureCountdownActive)
        assertTrue(result.triggerCapture)
    }

    @Test
    fun flowTickSignalsAndroidTimeoutsAndKeepsPrintingStatusRules() {
        val sessionTimeout = advanceFlowTick(
            FlowTickState(SessionStage.PREVIEW, 10, 1)
        )
        val qrTimeout = advanceFlowTick(
            FlowTickState(SessionStage.QR, 10, 20, qrExpirySeconds = 1)
        )
        val printCompleted = advanceFlowTick(
            FlowTickState(SessionStage.PRINTING, 10, 20, printProgress = 1f)
        )
        val printFailed = advanceFlowTick(
            FlowTickState(SessionStage.PRINTING, 10, 20, printStatus = "Export failed")
        )

        assertTrue(sessionTimeout.returnToLanding)
        assertTrue(qrTimeout.returnToLanding)
        assertEquals("PDF created successfully", printCompleted.state.printStatus)
        assertEquals("Export failed", printFailed.state.printStatus)
    }

    @Test
    fun flowTickAdvancesStagesAndAutofillsTimedOutAssignments() {
        val cameraTimeout = advanceFlowTick(
            FlowTickState(SessionStage.CAMERA_MODE, 1, 50),
            FlowTimerSettings(captureSeconds = 75)
        ).state
        val captureTimeout = advanceFlowTick(
            FlowTickState(SessionStage.CAPTURE, 1, 50, isCaptureInProgress = true, isCaptureCountdownActive = true, captureCountdown = 2),
            FlowTimerSettings(stripSizeSeconds = 18)
        ).state
        val stripTimeout = advanceFlowTick(
            FlowTickState(SessionStage.STRIP_SIZE, 1, 50),
            FlowTimerSettings(photoAssignmentSeconds = 21)
        ).state
        val assignmentTimeout = advanceFlowTick(
            FlowTickState(
                stage = SessionStage.PHOTO_ASSIGNMENT,
                stageSecondsLeft = 1,
                sessionSecondsLeft = 50,
                capturedPhotoCount = 3,
                assignments = listOf(1, null, null, null),
                assignmentListVisible = true
            ),
            FlowTimerSettings(previewSeconds = 40),
            assignmentFillOrder = listOf(2, 0, 1)
        ).state

        assertEquals(SessionStage.CAPTURE, cameraTimeout.stage)
        assertEquals(75, cameraTimeout.stageSecondsLeft)
        assertEquals("Capture session ready", cameraTimeout.status)
        assertEquals(SessionStage.STRIP_SIZE, captureTimeout.stage)
        assertEquals(18, captureTimeout.stageSecondsLeft)
        assertFalse(captureTimeout.isCaptureInProgress)
        assertFalse(captureTimeout.isCaptureCountdownActive)
        assertEquals("Time expired, choose strip size", captureTimeout.status)
        assertEquals(SessionStage.PHOTO_ASSIGNMENT, stripTimeout.stage)
        assertEquals(21, stripTimeout.stageSecondsLeft)
        assertEquals("Time expired, assign photos", stripTimeout.status)
        assertEquals(SessionStage.PREVIEW, assignmentTimeout.stage)
        assertEquals(40, assignmentTimeout.stageSecondsLeft)
        assertEquals(listOf(1, 2, 0, null), assignmentTimeout.assignments)
        assertEquals("Time expired, review the strip", assignmentTimeout.status)
        assertFalse(assignmentTimeout.assignmentListVisible)

        val allSlotsFull = advanceFlowTick(
            FlowTickState(
                stage = SessionStage.PHOTO_ASSIGNMENT,
                stageSecondsLeft = 1,
                sessionSecondsLeft = 50,
                capturedPhotoCount = 4,
                assignments = listOf(0, 1, 2),
                transforms = List(3) { PhotoTransform(scale = 2f) },
                assignmentListVisible = true
            ),
            assignmentFillOrder = listOf(3)
        ).state
        assertEquals(listOf(0, 1, 2), allSlotsFull.assignments)
        assertEquals(List(3) { PhotoTransform() }, allSlotsFull.transforms)
        assertFalse(allSlotsFull.assignmentListVisible)
    }

    @Test
    fun photoTransformClampsScaleToEditorRange() {
        assertEquals(0.5f, PhotoTransform().withScale(0f).scale)
        assertEquals(5f, PhotoTransform().withScale(8f).scale)
        assertEquals(2f, PhotoTransform().withScale(2f).scale)
    }
}
