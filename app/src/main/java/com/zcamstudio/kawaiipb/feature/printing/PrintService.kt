package com.zcamstudio.kawaiipb.feature.printing

import com.zcamstudio.kawaiipb.feature.flow.presentation.FlowUiState
import com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService

object PrintService {
    fun renderPrintSheet(uiState: FlowUiState, storageService: KawaiiStorageService): Boolean {
        return PrintComposer.renderPrintSheet(uiState, storageService)
    }
}
