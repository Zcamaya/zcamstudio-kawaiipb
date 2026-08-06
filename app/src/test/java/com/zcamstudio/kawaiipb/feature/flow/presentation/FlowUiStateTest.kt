package com.zcamstudio.kawaiipb.feature.flow.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class FlowUiStateTest {

    @Test
    fun defaultSessionDurationIsThirtyMinutes() {
        assertEquals(60 * 30, FlowUiState().sessionSecondsLeft)
    }
}
