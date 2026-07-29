package com.zcamstudio.kawaiipb.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zcamstudio.kawaiipb.domain.usecase.GetAdminDashboardSummaryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminViewModel(
    private val getAdminDashboardSummaryUseCase: GetAdminDashboardSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val summary = getAdminDashboardSummaryUseCase()
            _uiState.update { it.copy(isLoading = false, summary = summary) }
        }
    }

    fun onSectionSelected(section: String) {
        _uiState.update { it.copy(selectedSection = section) }
    }
}
