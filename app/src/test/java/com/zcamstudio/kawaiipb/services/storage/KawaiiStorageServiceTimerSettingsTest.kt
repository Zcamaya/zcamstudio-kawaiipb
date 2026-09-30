package com.zcamstudio.kawaiipb.services.storage

import com.zcamstudio.kawaiipb.feature.flow.presentation.FlowTimerSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties

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

    @Test
    fun disabledStripLayoutPathsRoundTripPreservesAssetAndWindowsPaths() {
        val paths = setOf(
            "asset://templates/Base",
            "C:\\Users\\Operator\\Downloads\\KawaiiPB\\Templates\\SpringBloom"
        )

        val persisted = KawaiiStorageService.disabledStripLayoutPathsFromProperties(
            Properties().apply {
                load(
                    StringReader(
                        StringWriter().also { writer ->
                            KawaiiStorageService.disabledStripLayoutPathsToProperties(paths).store(writer, null)
                        }.toString()
                    )
                )
            }
        )

        assertEquals(paths, persisted)
    }
}
