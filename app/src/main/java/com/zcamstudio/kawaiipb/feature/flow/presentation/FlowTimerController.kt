package com.zcamstudio.kawaiipb.feature.flow.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class FlowTicker(
    private val scope: CoroutineScope,
    private val onTick: () -> Unit
) {
    private var tickerJob: Job? = null

    fun start() {
        if (tickerJob != null) return
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000)
                onTick()
            }
        }
    }
}
