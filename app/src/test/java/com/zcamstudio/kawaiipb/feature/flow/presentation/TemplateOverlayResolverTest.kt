package com.zcamstudio.kawaiipb.feature.flow.presentation

import com.zcamstudio.kawaiipb.domain.model.StripSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class TemplateOverlayResolverTest {
    @Test
    fun `finds overlay files from a template folder for matching strip size`() {
        val tempDir = createTempDirectory("template-overlays").toFile()
        val templateDir = File(tempDir, "SpringBloom")
        val sizeDir = File(templateDir, "2x4")
        sizeDir.mkdirs()
        File(templateDir, "background.png").writeBytes(byteArrayOf(1, 2, 3))
        File(sizeDir, "overlay.png").writeBytes(byteArrayOf(4, 5, 6))
        File(sizeDir, "ignore.txt").writeText("ignore")

        val overlays = listTemplateOverlayOptions(tempDir, StripSize.TwoByFour)

        assertEquals(1, overlays.size)
        assertEquals("SpringBloom/overlay", overlays.first().displayName)
        assertEquals("overlay.png", overlays.first().fileName)
        assertTrue(overlays.first().path.replace(File.separatorChar, '/').endsWith("SpringBloom/2x4/overlay.png"))
    }

    @Test
    fun `returns empty list when no overlay exists for strip size`() {
        val tempDir = createTempDirectory("template-overlays-empty").toFile()
        val sizeDir = File(tempDir, "2x3")
        sizeDir.mkdirs()
        File(sizeDir, "overlay.png").writeBytes(byteArrayOf(1, 2, 3))

        val overlays = listTemplateOverlayOptions(tempDir, StripSize.TwoByFour)

        assertTrue(overlays.isEmpty())
    }
}
