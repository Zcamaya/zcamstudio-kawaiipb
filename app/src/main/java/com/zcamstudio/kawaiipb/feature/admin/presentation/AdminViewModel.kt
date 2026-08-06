package com.zcamstudio.kawaiipb.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zcamstudio.kawaiipb.domain.model.CameraMode
import com.zcamstudio.kawaiipb.domain.usecase.GetAdminDashboardSummaryUseCase
import com.zcamstudio.kawaiipb.feature.flow.presentation.FlowTimerSettings
import com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminViewModel(
    private val getAdminDashboardSummaryUseCase: GetAdminDashboardSummaryUseCase,
    private val storageService: KawaiiStorageService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val summary = getAdminDashboardSummaryUseCase()
            val savedSettings = storageService.loadFlowTimerSettings()
            val savedCameraSelections = storageService.loadCameraModeSelections()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    summary = summary,
                    cameraModeDuration = savedSettings.cameraModeDuration,
                    captureDuration = savedSettings.captureDuration,
                    photoAssignmentDuration = savedSettings.photoAssignmentDuration,
                    stripSizeDuration = savedSettings.stripSizeDuration,
                    previewDuration = savedSettings.previewDuration,
                    qrCodeDuration = savedSettings.qrCodeDuration,
                    preCaptureDelaySeconds = savedSettings.preCaptureDelaySeconds,
                    classicCameraSelectionId = savedCameraSelections[CameraMode.Classic] ?: "front",
                    elevatorCameraSelectionId = savedCameraSelections[CameraMode.Elevator] ?: "rear"
                )
            }
        }
    }

    fun onSectionSelected(section: String) {
        _uiState.update { it.copy(selectedSection = section) }
    }

    fun onCameraSelectionChanged(mode: String, cameraId: String) {
        _uiState.update { state ->
            val updatedState = when (mode) {
                "classic" -> state.copy(classicCameraSelectionId = cameraId)
                "elevator" -> state.copy(elevatorCameraSelectionId = cameraId)
                else -> state
            }
            saveCameraModeSelections(updatedState)
            updatedState.copy(statusMessage = "Camera saved")
        }
    }

    fun onTimerSettingChanged(key: String, value: Int) {
        _uiState.update { state ->
            val clampedValue = when (key) {
                "preCaptureDelay" -> value.coerceIn(0, 10)
                else -> value.coerceIn(5, 240)
            }
            val updatedState = when (key) {
                "cameraMode" -> state.copy(cameraModeDuration = clampedValue)
                "capture" -> state.copy(captureDuration = clampedValue)
                "photoAssignment" -> state.copy(photoAssignmentDuration = clampedValue)
                "stripSize" -> state.copy(stripSizeDuration = clampedValue)
                "preview" -> state.copy(previewDuration = clampedValue)
                "qrCode" -> state.copy(qrCodeDuration = clampedValue)
                "preCaptureDelay" -> state.copy(preCaptureDelaySeconds = clampedValue)
                else -> state
            }

            saveCurrentSettings(updatedState)
            updatedState.copy(statusMessage = "Timer saved")
        }
    }

    fun onSaveTimerSettings() {
        _uiState.update { state ->
            saveCurrentSettings(state)
            state.copy(statusMessage = "Settings saved")
        }
    }

    fun onResetTimerSettings() {
        val defaultSettings = FlowTimerSettings()
        _uiState.update { state ->
            state.copy(
                cameraModeDuration = defaultSettings.cameraModeDuration,
                captureDuration = defaultSettings.captureDuration,
                photoAssignmentDuration = defaultSettings.photoAssignmentDuration,
                stripSizeDuration = defaultSettings.stripSizeDuration,
                previewDuration = defaultSettings.previewDuration,
                qrCodeDuration = defaultSettings.qrCodeDuration,
                preCaptureDelaySeconds = defaultSettings.preCaptureDelaySeconds,
                statusMessage = "Defaults restored"
            )
        }
        storageService.saveFlowTimerSettings(defaultSettings)
    }

    private fun saveCurrentSettings(state: AdminUiState) {
        val settings = FlowTimerSettings(
            cameraModeDuration = state.cameraModeDuration.coerceIn(5, 240),
            captureDuration = state.captureDuration.coerceIn(5, 240),
            photoAssignmentDuration = state.photoAssignmentDuration.coerceIn(5, 240),
            stripSizeDuration = state.stripSizeDuration.coerceIn(5, 240),
            previewDuration = state.previewDuration.coerceIn(5, 240),
            qrCodeDuration = state.qrCodeDuration.coerceIn(5, 240),
            preCaptureDelaySeconds = state.preCaptureDelaySeconds.coerceIn(0, 10)
        )
        storageService.saveFlowTimerSettings(settings)
    }

    private fun saveCameraModeSelections(state: AdminUiState) {
        storageService.saveCameraModeSelections(
            mapOf(
                CameraMode.Classic to state.classicCameraSelectionId,
                CameraMode.Elevator to state.elevatorCameraSelectionId
            )
        )
    }
}
