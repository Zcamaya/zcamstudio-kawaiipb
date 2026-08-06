package com.zcamstudio.kawaiipb.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zcamstudio.kawaiipb.app.LocalKawaiiPbDependencies
import com.zcamstudio.kawaiipb.core.viewmodel.KawaiiViewModelFactory
import com.zcamstudio.kawaiipb.feature.admin.presentation.AdminScreen
import com.zcamstudio.kawaiipb.feature.admin.presentation.AdminViewModel
import com.zcamstudio.kawaiipb.feature.flow.presentation.FlowEffect
import com.zcamstudio.kawaiipb.feature.flow.presentation.FlowScreen
import com.zcamstudio.kawaiipb.feature.flow.presentation.FlowViewModel
import com.zcamstudio.kawaiipb.feature.landing.presentation.LandingEffect
import com.zcamstudio.kawaiipb.feature.landing.presentation.LandingScreen
import com.zcamstudio.kawaiipb.feature.landing.presentation.LandingViewModel

object KawaiiRoutes {
    const val Landing = "landing"
    const val Flow = "flow/{sessionId}"
    const val Admin = "admin"

    fun flow(sessionId: String): String = "flow/${Uri.encode(sessionId)}"
}

@Composable
fun KawaiiNavHost(
    navController: NavHostController = rememberNavController()
) {
    val dependencies = LocalKawaiiPbDependencies.current

    NavHost(
        navController = navController,
        startDestination = KawaiiRoutes.Landing
    ) {
        composable(KawaiiRoutes.Landing) {
            val viewModel: LandingViewModel = viewModel(
                factory = remember(dependencies) {
                    KawaiiViewModelFactory {
                        LandingViewModel(
                            getLandingConfigUseCase = dependencies.getLandingConfigUseCase,
                            sessionLogService = dependencies.sessionLogService
                        )
                    }
                }
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is LandingEffect.NavigateToFlow -> navController.navigate(KawaiiRoutes.flow(effect.sessionId))
                        LandingEffect.NavigateToAdmin -> navController.navigate(KawaiiRoutes.Admin)
                    }
                }
            }

            LandingScreen(
                uiState = uiState,
                onStartClicked = viewModel::onStartClicked,
                onLogoTapped = viewModel::onLogoTapped,
                onAdminUnlockConfirmed = viewModel::onAdminUnlockConfirmed
            )
        }

        composable(
            route = KawaiiRoutes.Flow,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId").orEmpty()
            val viewModel: FlowViewModel = viewModel(
                factory = remember(dependencies, sessionId) {
                    KawaiiViewModelFactory {
                        FlowViewModel(
                            sessionId = sessionId,
                            getKioskSessionCatalogUseCase = dependencies.getKioskSessionCatalogUseCase,
                            storageService = dependencies.storageService,
                            sessionLogService = dependencies.sessionLogService
                        )
                    }
                }
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        FlowEffect.ReturnToLanding -> {
                            navController.popBackStack(KawaiiRoutes.Landing, inclusive = false)
                        }
                    }
                }
            }

            FlowScreen(
                uiState = uiState,
                onSelectCameraMode = viewModel::selectCameraMode,
                onContinueFromCameraMode = viewModel::continueFromCameraMode,
                onCaptureNow = viewModel::captureFrameNow,
                onCameraReadyChanged = viewModel::markCameraReady,
                onCaptureSucceeded = viewModel::onCaptureSucceeded,
                onCaptureFailed = viewModel::onCaptureFailed,
                onContinueFromCaptureComplete = viewModel::continueFromCaptureComplete,
                onContinueFromPhotoAssignment = viewModel::continueFromPhotoAssignment,
                onToggleAutoCapture = viewModel::toggleAutoCapture,
                onSelectStripSize = viewModel::selectStripSize,
                onContinueStripSize = viewModel::continueStripSize,
                onLoadStripLayout = viewModel::setStripLayout,
                onSelectAssignedFrame = viewModel::selectAssignedFrame,
                onRemoveFrame = viewModel::removePhotoFromFrame,
                onSelectCapturedPhoto = viewModel::selectCapturedPhoto,
                onUpdatePhotoAssignmentTransform = viewModel::updatePhotoAssignmentTransform,
                onResetPhotoAssignmentTransform = viewModel::resetPhotoAssignmentTransform,
                onShuffleAssignment = viewModel::shufflePhotoAssignments,
                onResetAssignment = viewModel::resetPhotoAssignments,
                onAutoFillAssignment = viewModel::autoFillPhotoAssignments,
                onBeginPrinting = viewModel::beginPrinting,
                onReturnToLanding = viewModel::returnToLanding,
                onOpenAdmin = {
                    navController.navigate(KawaiiRoutes.Admin)
                }
            )
        }

        composable(KawaiiRoutes.Admin) {
            val viewModel: AdminViewModel = viewModel(
                factory = remember(dependencies) {
                    KawaiiViewModelFactory {
                        AdminViewModel(
                            getAdminDashboardSummaryUseCase = dependencies.getAdminDashboardSummaryUseCase,
                            storageService = dependencies.storageService
                        )
                    }
                }
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AdminScreen(
                uiState = uiState,
                onSectionSelected = viewModel::onSectionSelected,
                onTimerSettingChanged = viewModel::onTimerSettingChanged,
                onCameraSelectionChanged = viewModel::onCameraSelectionChanged,
                onSaveTimerSettings = viewModel::onSaveTimerSettings,
                onResetTimerSettings = viewModel::onResetTimerSettings,
                onBackToLanding = {
                    navController.popBackStack(KawaiiRoutes.Landing, inclusive = false)
                }
            )
        }
    }
}
