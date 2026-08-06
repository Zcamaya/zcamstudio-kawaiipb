package com.zcamstudio.kawaiipb.services.storage

import com.zcamstudio.kawaiipb.feature.flow.presentation.FlowTimerSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class KawaiiStorageServiceTimerSettingsTest {

    @Test
    fun flowTimerSettingsRoundTripPreservesValues() {
        val settings = FlowTimerSettings(
            cameraModeDuration = 15,
            captureDuration = 45,
            photoAssignmentDuration = 30,
            stripSizeDuration = 12,
            previewDuration = 22,
            qrCodeDuration = 45,
            preCaptureDelaySeconds = 5
        )

        val persisted = KawaiiStorageService.flowTimerSettingsFromMap(
            KawaiiStorageService.flowTimerSettingsToMap(settings)
        )

        assertEquals(settings, persisted)
    }
}
