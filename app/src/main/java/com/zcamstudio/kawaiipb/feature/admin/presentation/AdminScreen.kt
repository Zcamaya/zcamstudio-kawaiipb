package com.zcamstudio.kawaiipb.feature.admin.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zcamstudio.kawaiipb.core.designsystem.CherryPink
import kotlin.math.roundToInt
import com.zcamstudio.kawaiipb.core.designsystem.InkRose
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiBackdrop
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiCard
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiPill
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiPrimaryButton
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiSecondaryButton
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiSectionTitle
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiSpacing
import com.zcamstudio.kawaiipb.core.designsystem.KioskLayoutMode
import com.zcamstudio.kawaiipb.core.designsystem.LineRose
import com.zcamstudio.kawaiipb.core.designsystem.MintFoam
import com.zcamstudio.kawaiipb.core.designsystem.SoftLavender
import com.zcamstudio.kawaiipb.core.designsystem.SoftText
import com.zcamstudio.kawaiipb.core.designsystem.WarmCream
import com.zcamstudio.kawaiipb.core.designsystem.rememberKioskLayoutMode
import com.zcamstudio.kawaiipb.domain.model.AdminDashboardSummary
import com.zcamstudio.kawaiipb.domain.model.ChartPoint
import com.zcamstudio.kawaiipb.domain.model.RankedItem
import com.zcamstudio.kawaiipb.domain.model.StatusBadge
import com.zcamstudio.kawaiipb.feature.flow.presentation.StripLayoutOption
import com.zcamstudio.kawaiipb.feature.flow.presentation.listStripLayoutOptions

private val PreCaptureDelayOptions = listOf(0, 3, 5, 7, 10)

private val AdminSections = listOf(
    "Dashboard",
    "Camera",
    "Strip Sizes",
    "Printer",
    "Storage",
    "Settings",
    "Logs"
)

private fun closestPreCaptureDelay(value: Int): Int =
    PreCaptureDelayOptions.minByOrNull { kotlin.math.abs(it - value) } ?: 5

@Composable
fun AdminScreen(
    uiState: AdminUiState,
    onSectionSelected: (String) -> Unit,
    onTimerSettingChanged: (String, Int) -> Unit,
    onCameraSelectionChanged: (String, String) -> Unit,
    onStripLayoutEnabledChanged: (String, Boolean) -> Unit,
    onSaveTimerSettings: () -> Unit,
    onResetTimerSettings: () -> Unit,
    onBackToLanding: () -> Unit
) {
    val layoutMode = rememberKioskLayoutMode()
    val isPortrait = layoutMode == KioskLayoutMode.Portrait
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val stripLayouts = remember(context) {
        listStripLayoutOptions(context)
            .distinctBy { it.layoutAssetPath }
            .sortedBy { it.displayName.lowercase() }
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    KawaiiBackdrop(modifier = Modifier.fillMaxSize())

    if (uiState.isLoading || uiState.summary == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_loading), style = MaterialTheme.typography.titleLarge)
        }
        return
    }

    val summary = uiState.summary
    val cameraOptions = listOf(
        CameraDeviceOption(id = "front", label = "Front Camera", isExternal = false),
        CameraDeviceOption(id = "rear", label = "Rear Camera", isExternal = false),
        CameraDeviceOption(id = "external", label = "External Camera", isExternal = true)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (isPortrait) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminSectionTabs(
                    selectedSection = uiState.selectedSection,
                    onSectionSelected = onSectionSelected,
                    onBackToLanding = onBackToLanding
                )
                if (uiState.selectedSection == "Settings") {
                    TimerSettingsCard(
                        uiState = uiState,
                        onTimerSettingChanged = onTimerSettingChanged,
                        onSaveTimerSettings = onSaveTimerSettings,
                        onResetTimerSettings = onResetTimerSettings
                    )
                } else if (uiState.selectedSection == "Camera") {
                    CameraSettingsCard(
                        uiState = uiState,
                        cameraOptions = cameraOptions,
                        onCameraSelectionChanged = onCameraSelectionChanged
                    )
                } else if (uiState.selectedSection == "Strip Sizes") {
                    StripSizeSettingsCard(
                        stripLayouts = stripLayouts,
                        disabledStripLayoutPaths = uiState.disabledStripLayoutPaths,
                        onStripLayoutEnabledChanged = onStripLayoutEnabledChanged
                    )
                } else {
                    PortraitDashboardContent(summary = summary)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LandscapeSidebar(
                    selectedSection = uiState.selectedSection,
                    onSectionSelected = onSectionSelected,
                    onBackToLanding = onBackToLanding
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (uiState.selectedSection == "Settings") {
                        TimerSettingsCard(
                            uiState = uiState,
                            onTimerSettingChanged = onTimerSettingChanged,
                            onSaveTimerSettings = onSaveTimerSettings,
                            onResetTimerSettings = onResetTimerSettings
                        )
                    } else if (uiState.selectedSection == "Camera") {
                        CameraSettingsCard(
                            uiState = uiState,
                            cameraOptions = cameraOptions,
                            onCameraSelectionChanged = onCameraSelectionChanged
                        )
                    } else if (uiState.selectedSection == "Strip Sizes") {
                        StripSizeSettingsCard(
                            stripLayouts = stripLayouts,
                            disabledStripLayoutPaths = uiState.disabledStripLayoutPaths,
                            onStripLayoutEnabledChanged = onStripLayoutEnabledChanged
                        )
                    } else {
                        LandscapeDashboardContent(summary = summary)
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun LandscapeSidebar(
    selectedSection: String,
    onSectionSelected: (String) -> Unit,
    onBackToLanding: () -> Unit
) {
    val sections = AdminSections
    Column(
        modifier = Modifier.width(200.dp).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KawaiiSectionTitle(title = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_panel_title), subtitle = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_panel_subtitle))
        Spacer(modifier = Modifier.height(6.dp))
        sections.forEach { section ->
            val isSelected = section == selectedSection
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isSelected) CherryPink.copy(alpha = 0.18f) else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CherryPink else LineRose)
            ) {
                Text(
                    text = section,
                    modifier = Modifier
                        .clickable { onSectionSelected(section) }
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 14.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = InkRose
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        KawaiiSecondaryButton(text = "Back to Landing", modifier = Modifier.fillMaxWidth()) {
            onBackToLanding()
        }
    }
}

@Composable
private fun AdminSectionTabs(
    selectedSection: String,
    onSectionSelected: (String) -> Unit,
    onBackToLanding: () -> Unit
) {
    val sections = AdminSections
    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KawaiiSectionTitle(
            title = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_panel_title),
            subtitle = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_panel_landscape_subtitle)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sections.forEach { section ->
                val isSelected = section == selectedSection
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = if (isSelected) CherryPink.copy(alpha = 0.18f) else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CherryPink else LineRose)
                ) {
                    Text(
                        text = section,
                        modifier = Modifier
                            .clickable { onSectionSelected(section) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = InkRose
                    )
                }
            }
        }
        KawaiiSecondaryButton(text = "Back to Landing", modifier = Modifier.fillMaxWidth()) {
            onBackToLanding()
        }
    }
}

@Composable
private fun TimerSettingsCard(
    uiState: AdminUiState,
    onTimerSettingChanged: (String, Int) -> Unit,
    onSaveTimerSettings: () -> Unit,
    onResetTimerSettings: () -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    KawaiiCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                KawaiiSectionTitle(title = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_timer_title), subtitle = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_timer_subtitle))
                KawaiiSecondaryButton(text = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_reset_defaults)) {
                    showResetConfirm = true
                }
            }
            val timerFields = listOf(
                Triple("Camera Mode", "cameraMode", 5f..240f to uiState.cameraModeDuration),
                Triple("Capture", "capture", 5f..240f to uiState.captureDuration),
                Triple("Photo Assignment", "photoAssignment", 5f..240f to uiState.photoAssignmentDuration),
                Triple("Strip Size", "stripSize", 5f..240f to uiState.stripSizeDuration),
                Triple("Preview", "preview", 5f..240f to uiState.previewDuration),
                Triple("QR Code", "qrCode", 5f..240f to uiState.qrCodeDuration),
                Triple("Camera Settings", "preCaptureDelay", 0f..10f to uiState.preCaptureDelaySeconds)
            )
            timerFields.forEach { (label, key, valuePair) ->
                val (range, currentValue) = valuePair
                Column(modifier = Modifier.fillMaxWidth()) {
                    val displayedValue = if (key == "preCaptureDelay") {
                        closestPreCaptureDelay(currentValue)
                    } else {
                        currentValue
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = label, style = MaterialTheme.typography.titleSmall, color = InkRose)
                        Text(text = "$displayedValue sec", style = MaterialTheme.typography.bodyMedium, color = SoftText)
                    }
                    if (key == "preCaptureDelay") {
                        val sliderValue = PreCaptureDelayOptions
                            .indexOf(displayedValue)
                            .takeIf { it >= 0 }
                            ?.toFloat()
                            ?: 0f
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                value = sliderValue,
                                onValueChange = { newValue ->
                                    val optionIndex = newValue.roundToInt().coerceIn(0, PreCaptureDelayOptions.lastIndex)
                                    val snappedValue = PreCaptureDelayOptions[optionIndex]
                                    onTimerSettingChanged(key, snappedValue)
                                },
                                valueRange = 0f..PreCaptureDelayOptions.lastIndex.toFloat(),
                                steps = PreCaptureDelayOptions.size - 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                PreCaptureDelayOptions.forEach { delay ->
                                    Text(text = "${delay}s", style = MaterialTheme.typography.labelMedium, color = SoftText)
                                }
                            }
                        }
                    } else {
                        Slider(
                            value = currentValue.toFloat(),
                            onValueChange = { newValue ->
                                onTimerSettingChanged(key, newValue.roundToInt())
                            },
                            valueRange = range,
                            steps = ((range.endInclusive - range.start).toInt() / 5) - 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            KawaiiPrimaryButton(
                text = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_save),
                modifier = Modifier.fillMaxWidth()
            ) {
                onSaveTimerSettings()
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_reset_title)) },
            text = { Text(stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_reset_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onResetTimerSettings()
                    showResetConfirm = false
                }) {
                    Text(stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_cancel))
                }
            }
        )
    }
}

@Composable
private fun CameraSettingsCard(
    uiState: AdminUiState,
    cameraOptions: List<CameraDeviceOption>,
    onCameraSelectionChanged: (String, String) -> Unit
) {
    KawaiiCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            KawaiiSectionTitle(title = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_camera_assignment_title), subtitle = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_camera_assignment_subtitle))
            Text(
                text = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_camera_assignment_message),
                style = MaterialTheme.typography.bodySmall,
                color = SoftText
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MintFoam.copy(alpha = 0.14f)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CameraModeDropdown(
                        title = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_camera_classic),
                        selectedId = uiState.classicCameraSelectionId,
                        options = cameraOptions,
                        onSelectionChanged = { cameraId -> onCameraSelectionChanged("classic", cameraId) }
                    )
                    CameraModeDropdown(
                        title = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_camera_elevator),
                        selectedId = uiState.elevatorCameraSelectionId,
                        options = cameraOptions,
                        onSelectionChanged = { cameraId -> onCameraSelectionChanged("elevator", cameraId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StripSizeSettingsCard(
    stripLayouts: List<StripLayoutOption>,
    disabledStripLayoutPaths: Set<String>,
    onStripLayoutEnabledChanged: (String, Boolean) -> Unit
) {
    KawaiiCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            KawaiiSectionTitle(
                title = "Strip Sizes",
                subtitle = "Choose which strip layouts appear in the photo booth."
            )
            if (stripLayouts.isEmpty()) {
                Text(text = "No strip layouts found.", style = MaterialTheme.typography.bodyMedium, color = SoftText)
            } else {
                val enabledCount = stripLayouts.count { it.layoutAssetPath !in disabledStripLayoutPaths }
                stripLayouts.forEachIndexed { index, option ->
                    val isEnabled = option.layoutAssetPath !in disabledStripLayoutPaths
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = option.displayName, style = MaterialTheme.typography.titleSmall, color = InkRose)
                            Text(
                                text = "${option.frameCount} photo${if (option.frameCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SoftText
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            enabled = !isEnabled || enabledCount > 1,
                            onCheckedChange = { enabled -> onStripLayoutEnabledChanged(option.layoutAssetPath, enabled) }
                        )
                    }
                    if (index < stripLayouts.lastIndex) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LineRose.copy(alpha = 0.7f)))
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraModeDropdown(
    title: String,
    selectedId: String,
    options: List<CameraDeviceOption>,
    onSelectionChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldWidth by remember { mutableStateOf(0) }
    val selectedOption = options.firstOrNull { it.id == selectedId } ?: options.first()
    val density = LocalDensity.current

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = InkRose)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    textFieldWidth = coordinates.size.width
                }
                .clickable { expanded = true },
            shape = MaterialTheme.shapes.small,
            color = Color.White.copy(alpha = 0.08f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = selectedOption.label, style = MaterialTheme.typography.bodyLarge, color = InkRose)
                    if (selectedOption.isExternal) {
                        Text(text = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_camera_external_device), style = MaterialTheme.typography.bodySmall, color = SoftText)
                    }
                }
                Text(text = "▼", style = MaterialTheme.typography.titleLarge, color = InkRose)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(density) { textFieldWidth.toDp() })
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(option.label, color = InkRose)
                            if (option.isExternal) {
                                Text(stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_camera_external_hint), style = MaterialTheme.typography.bodySmall, color = SoftText)
                            }
                        }
                    },
                    onClick = {
                        onSelectionChanged(option.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LandscapeDashboardContent(summary: AdminDashboardSummary) {
    DashboardHeader()
    LandscapeMetricRow(metrics = summary.metrics)
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
        TrendCard(points = summary.sessionsTrend, modifier = Modifier.weight(1.35f))
        // Templates removed
    }
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
        // Stickers removed
        StatusCard(statuses = summary.statuses, modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
        // Templates manager removed
        MiniModuleCard(
            title = "Camera & Printer",
            subtitle = "Local hardware controls, presets, and test actions.",
            primary = "Detect Devices",
            secondary = "Open Settings",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PortraitDashboardContent(summary: AdminDashboardSummary) {
    DashboardHeader()
    PortraitMetricGrid(metrics = summary.metrics)
    TrendCard(points = summary.sessionsTrend, modifier = Modifier.fillMaxWidth())
    // Templates and Stickers removed from admin UI
    StatusCard(statuses = summary.statuses, modifier = Modifier.fillMaxWidth())
    // Templates manager removed
    MiniModuleCard(
        title = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_module_camera_printer_title),
        subtitle = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_module_camera_printer_subtitle),
        primary = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_module_detect_devices),
        secondary = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_module_open_settings),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DashboardHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_dashboard_title), style = MaterialTheme.typography.headlineLarge, color = InkRose)
            Text(text = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_dashboard_subtitle), style = MaterialTheme.typography.bodyMedium, color = SoftText)
        }
        KawaiiPill(text = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_dashboard_mode), accent = SoftLavender)
    }
}

@Composable
private fun LandscapeMetricRow(metrics: List<com.zcamstudio.kawaiipb.domain.model.DashboardMetric>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        metrics.forEach { metric ->
            MetricCard(metric = metric, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PortraitMetricGrid(metrics: List<com.zcamstudio.kawaiipb.domain.model.DashboardMetric>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        metrics.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { metric ->
                    MetricCard(metric = metric, modifier = Modifier.weight(1f))
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(metric: com.zcamstudio.kawaiipb.domain.model.DashboardMetric, modifier: Modifier = Modifier) {
    KawaiiCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = metric.label, style = MaterialTheme.typography.bodyMedium, color = SoftText)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = metric.value, style = MaterialTheme.typography.headlineMedium, color = InkRose)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = metric.delta, style = MaterialTheme.typography.labelLarge, color = SoftText)
        }
    }
}

@Composable
private fun TrendCard(points: List<ChartPoint>, modifier: Modifier = Modifier) {
    KawaiiCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            KawaiiSectionTitle(title = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_sessions_overview_title), subtitle = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_sessions_overview_subtitle))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val max = points.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
                points.forEach { point ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(((point.value / max.toFloat()) * 150f).dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(CherryPink, SoftLavender)
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = point.label, style = MaterialTheme.typography.labelLarge, color = SoftText)
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingCard(
    title: String,
    items: List<RankedItem>,
    accent: Color,
    modifier: Modifier = Modifier
) {
    KawaiiCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            KawaiiSectionTitle(title = title, subtitle = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_ranking_subtitle))
            items.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = item.label, style = MaterialTheme.typography.bodyMedium, color = InkRose)
                        Text(text = "${item.percentage}%", style = MaterialTheme.typography.labelLarge, color = SoftText)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(WarmCream)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = item.percentage / 100f)
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(accent, CherryPink)))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(statuses: List<StatusBadge>, modifier: Modifier = Modifier) {
    KawaiiCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            KawaiiSectionTitle(title = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_status_title), subtitle = stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_status_subtitle))
            statuses.forEach { status ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = status.label, style = MaterialTheme.typography.bodyLarge, color = InkRose)
                        Text(text = status.details, style = MaterialTheme.typography.bodyMedium, color = SoftText)
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (status.isHealthy) MintFoam else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = if (status.isHealthy) stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_status_ok) else stringResource(id = com.zcamstudio.kawaiipb.R.string.admin_status_alert),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = InkRose
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniModuleCard(
    title: String,
    subtitle: String,
    primary: String,
    secondary: String,
    modifier: Modifier = Modifier
) {
    KawaiiCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            KawaiiSectionTitle(title = title, subtitle = subtitle)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KawaiiPrimaryButton(text = primary, modifier = Modifier.weight(1f)) {}
                KawaiiSecondaryButton(text = secondary, modifier = Modifier.weight(1f)) {}
            }
        }
    }
}
