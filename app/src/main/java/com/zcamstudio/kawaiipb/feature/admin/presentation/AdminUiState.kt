package com.zcamstudio.kawaiipb.feature.admin.presentation

import com.zcamstudio.kawaiipb.domain.model.AdminDashboardSummary

data class AdminUiState(
    val isLoading: Boolean = true,
    val summary: AdminDashboardSummary? = null,
    val selectedSection: String = "Dashboard"
)
