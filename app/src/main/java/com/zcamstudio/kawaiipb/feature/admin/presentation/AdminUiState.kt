package com.zcamstudio.kawaiipb.feature.admin.presentation

import com.zcamstudio.kawaiipb.domain.model.AdminDashboardSummary

data class CameraDeviceOption(
    val id: String,
    val label: String,
    val isExternal: Boolean = false
)

data class AdminUiState(
    val isLoading: Boolean = true,
    val summary: AdminDashboardSummary? = null,
    val selectedSection: String = "Dashboard",
    val statusMessage: String? = null,
    val disabledStripLayoutPaths: Set<String> = emptySet(),
    val cameraModeDuration: Int = 20,
    val captureDuration: Int = 90,
    val photoAssignmentDuration: Int = 25,
    val stripSizeDuration: Int = 20,
    val previewDuration: Int = 25,
    val qrCodeDuration: Int = 30,
    val preCaptureDelaySeconds: Int = 5,
    val classicCameraSelectionId: String = "front",
    val elevatorCameraSelectionId: String = "rear"
)
