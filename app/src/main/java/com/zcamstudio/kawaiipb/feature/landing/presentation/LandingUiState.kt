package com.zcamstudio.kawaiipb.feature.landing.presentation

import com.zcamstudio.kawaiipb.domain.model.LandingConfig

data class LandingUiState(
    val isLoading: Boolean = true,
    val config: LandingConfig = LandingConfig(),
    val logoTapCount: Int = 0,
    val showAdminPinDialog: Boolean = false,
    val pinInput: String = "",
    val pinError: String? = null
)
