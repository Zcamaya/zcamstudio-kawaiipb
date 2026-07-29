package com.zcamstudio.kawaiipb.feature.landing.presentation

sealed interface LandingEffect {
    data class NavigateToFlow(val sessionId: String) : LandingEffect
    data object NavigateToAdmin : LandingEffect
}
