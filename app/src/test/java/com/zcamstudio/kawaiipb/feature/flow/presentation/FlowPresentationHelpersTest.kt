package com.zcamstudio.kawaiipb.feature.flow.presentation

import com.zcamstudio.kawaiipb.domain.model.KioskFlowStage
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowPresentationHelpersTest {

    @Test
    fun stageDurationUsesExpectedDurations() {
        assertEquals(20, stageDuration(KioskFlowStage.CameraMode))
        assertEquals(90, stageDuration(KioskFlowStage.Capture))
        assertEquals(300, stageDuration(KioskFlowStage.Qr))
    }

    @Test
    fun stageTextHelpersReturnExpectedLabels() {
        assertEquals("Choose Camera", flowStageTitle(KioskFlowStage.CameraMode))
        assertEquals("Preview", flowStageTitle(KioskFlowStage.Preview))
        assertEquals("Check the final composition.", flowStageSubtitle(KioskFlowStage.Preview))
    }
}
