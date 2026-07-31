package com.zcamstudio.kawaiipb.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.zcamstudio.kawaiipb.data.repository.InMemoryKioskRepository
import com.zcamstudio.kawaiipb.domain.repository.KioskRepository
import com.zcamstudio.kawaiipb.domain.usecase.GetAdminDashboardSummaryUseCase
import com.zcamstudio.kawaiipb.domain.usecase.GetFlowStepsUseCase
import com.zcamstudio.kawaiipb.domain.usecase.GetKioskSessionCatalogUseCase
import com.zcamstudio.kawaiipb.domain.usecase.GetLandingConfigUseCase
import com.zcamstudio.kawaiipb.domain.usecase.ValidateAdminPinUseCase
import com.zcamstudio.kawaiipb.services.logging.SessionLogService
import com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService

data class KawaiiPbDependencies(
    val repository: KioskRepository,
    val storageService: KawaiiStorageService,
    val sessionLogService: SessionLogService,
    val getLandingConfigUseCase: GetLandingConfigUseCase,
    val validateAdminPinUseCase: ValidateAdminPinUseCase,
    val getAdminDashboardSummaryUseCase: GetAdminDashboardSummaryUseCase,
    val getFlowStepsUseCase: GetFlowStepsUseCase,
    val getKioskSessionCatalogUseCase: GetKioskSessionCatalogUseCase
)

val LocalKawaiiPbDependencies = staticCompositionLocalOf<KawaiiPbDependencies> {
    error("KawaiiPbDependencies were not provided.")
}

@Composable
fun rememberKawaiiPbDependencies(): KawaiiPbDependencies {
    val context = LocalContext.current
    val repository = remember { InMemoryKioskRepository() }
    val storageService = remember(context) { KawaiiStorageService(context) }
    storageService.initializePublicFolders()
    val sessionLogService = remember(storageService) { SessionLogService(storageService) }
    return remember(repository, storageService, sessionLogService) {
        KawaiiPbDependencies(
            repository = repository,
            storageService = storageService,
            sessionLogService = sessionLogService,
            getLandingConfigUseCase = GetLandingConfigUseCase(repository),
            validateAdminPinUseCase = ValidateAdminPinUseCase(repository),
            getAdminDashboardSummaryUseCase = GetAdminDashboardSummaryUseCase(repository),
            getFlowStepsUseCase = GetFlowStepsUseCase(repository),
            getKioskSessionCatalogUseCase = GetKioskSessionCatalogUseCase(repository)
        )
    }
}
