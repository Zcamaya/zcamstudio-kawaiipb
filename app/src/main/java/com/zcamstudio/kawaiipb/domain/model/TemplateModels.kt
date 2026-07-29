package com.zcamstudio.kawaiipb.domain.model

import java.io.File

// Manifest model for template packages
data class TemplateManifest(
    val id: String,
    val name: String,
    val stripType: String,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val photoSlots: List<TemplatePhotoSlot>
)

data class TemplatePhotoSlot(
    val id: Int,
    val strip: Int = 1,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val rotation: Float = 0f,
    val mask: String? = null,
    val visible: Boolean = true
)

// Represents a loaded template package on disk
data class TemplatePackage(
    val manifest: TemplateManifest,
    val templateImage: File?,
    val previewImage: File?,
    val thumbnailImage: File?
)

// Represents a generated base layout (strip layout) for a session
data class StripLayout(
    val stripType: String,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val photoSlots: List<TemplatePhotoSlot>,
    val backgroundImage: String? = null,
    val doubleStrip: Boolean = false,
    val safeArea: LayoutSafeArea? = null,
    val brandingArea: LayoutBrandingArea? = null
)

data class LayoutSafeArea(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

data class LayoutBrandingArea(
    val enabled: Boolean,
    val height: Int
)
