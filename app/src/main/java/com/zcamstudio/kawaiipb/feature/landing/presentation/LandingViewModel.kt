package com.zcamstudio.kawaiipb.feature.landing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zcamstudio.kawaiipb.domain.usecase.GetLandingConfigUseCase
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
        val nextCount = _uiState.value.logoTapCount + 1
        _uiState.update { it.copy(logoTapCount = nextCount, pinError = null) }
    }

    fun onAdminUnlockConfirmed() {
        viewModelScope.launch {
            _uiState.update { it.copy(logoTapCount = 0) }
            _effects.emit(LandingEffect.NavigateToAdmin)
        }
    }

    private fun buildSessionId(): String {
        return "KPB-${System.currentTimeMillis()}"
    }
}
