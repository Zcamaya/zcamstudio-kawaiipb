package com.zcamstudio.kawaiipb.feature.landing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zcamstudio.kawaiipb.domain.usecase.GetLandingConfigUseCase
import com.zcamstudio.kawaiipb.domain.usecase.ValidateAdminPinUseCase
import com.zcamstudio.kawaiipb.services.logging.SessionLogService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LandingViewModel(
    private val getLandingConfigUseCase: GetLandingConfigUseCase,
    private val validateAdminPinUseCase: ValidateAdminPinUseCase,
    private val sessionLogService: SessionLogService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LandingUiState())
    val uiState: StateFlow<LandingUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<LandingEffect>()
    val effects: SharedFlow<LandingEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val config = getLandingConfigUseCase()
            _uiState.update { it.copy(isLoading = false, config = config) }
        }
    }

    fun onStartClicked() {
        viewModelScope.launch {
            val sessionId = buildSessionId()
            sessionLogService.logSessionStart(sessionId)
            _effects.emit(LandingEffect.NavigateToFlow(sessionId))
        }
    }

    fun onLogoTapped() {
        _uiState.update { state ->
            val nextCount = state.logoTapCount + 1
            state.copy(
                logoTapCount = nextCount,
                showAdminPinDialog = nextCount >= 7,
                pinError = null
            )
        }
    }

    fun onPinChanged(value: String) {
        _uiState.update { it.copy(pinInput = value.take(4), pinError = null) }
    }

    fun onPinDialogDismissed() {
        _uiState.update {
            it.copy(
                showAdminPinDialog = false,
                logoTapCount = 0,
                pinInput = "",
                pinError = null
            )
        }
    }

    fun onPinSubmitted() {
        viewModelScope.launch {
            val currentPin = uiState.value.pinInput
            val isValid = validateAdminPinUseCase(currentPin)
            if (isValid) {
                _uiState.update {
                    it.copy(
                        showAdminPinDialog = false,
                        logoTapCount = 0,
                        pinInput = "",
                        pinError = null
                    )
                }
                _effects.emit(LandingEffect.NavigateToAdmin)
            } else {
                _uiState.update { it.copy(pinError = "Invalid PIN, please try again.") }
            }
        }
    }

    private fun buildSessionId(): String {
        return "KPB-${System.currentTimeMillis()}"
    }
}
