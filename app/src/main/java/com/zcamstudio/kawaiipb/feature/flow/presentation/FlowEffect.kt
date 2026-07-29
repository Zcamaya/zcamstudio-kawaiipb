package com.zcamstudio.kawaiipb.feature.flow.presentation

sealed interface FlowEffect {
    data object ReturnToLanding : FlowEffect
}
