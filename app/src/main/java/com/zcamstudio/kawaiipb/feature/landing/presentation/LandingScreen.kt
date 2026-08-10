package com.zcamstudio.kawaiipb.feature.landing.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zcamstudio.kawaiipb.core.designsystem.CherryPink
import com.zcamstudio.kawaiipb.core.designsystem.CloudWhite
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiBackdrop
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiMascot
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiPrimaryButton
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiSpacing
import com.zcamstudio.kawaiipb.core.designsystem.KioskLayoutMode
import com.zcamstudio.kawaiipb.core.designsystem.SoftLavender
import com.zcamstudio.kawaiipb.core.designsystem.WarmCream
import com.zcamstudio.kawaiipb.core.designsystem.rememberKioskLayoutMode

@Composable
fun LandingScreen(
    uiState: LandingUiState,
    onStartClicked: () -> Unit,
    onLogoTapped: () -> Unit,
    onAdminUnlockConfirmed: () -> Unit
) {
    KawaiiBackdrop(modifier = Modifier.fillMaxSize())

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val layoutMode = rememberKioskLayoutMode()
        val isPortrait = layoutMode == KioskLayoutMode.Portrait
        val logoSize = if (isPortrait) 220.dp else 280.dp
        val mascotSize = if (isPortrait) 160.dp else 200.dp
        val titleStyle = if (maxWidth < 900.dp || isPortrait) {
            MaterialTheme.typography.headlineLarge
        } else {
            MaterialTheme.typography.displayLarge
        }
        val buttonWidthFraction = if (isPortrait) 0.74f else 0.36f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(logoSize)
                        .clip(CircleShape)
                        .clickable(onClick = onLogoTapped),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = WarmCream,
                        border = BorderStroke(1.dp, SoftLavender.copy(alpha = 0.55f)),
                        shadowElevation = 8.dp
                    ) {
                        Box(
                            modifier = Modifier.size(logoSize - 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            KawaiiMascot(modifier = Modifier.size(mascotSize))
                        }
                    }
                }

                Text(
                    text = stringResource(id = com.zcamstudio.kawaiipb.R.string.landing_title),
                    style = titleStyle,
                    color = CherryPink
                )

                KawaiiPrimaryButton(
                    text = stringResource(id = com.zcamstudio.kawaiipb.R.string.landing_start_session),
                    modifier = Modifier.fillMaxWidth(buttonWidthFraction)
                ) {
                    onStartClicked()
                }
            }
        }

        if (uiState.logoTapCount >= 7) {
            AdminUnlockDialog(
                onConfirm = onAdminUnlockConfirmed,
                onDismiss = { }
            )
        }
    }
}

@Composable
private fun AdminUnlockDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.38f),
            shape = MaterialTheme.shapes.large,
            color = CloudWhite.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, SoftLavender.copy(alpha = 0.55f)),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(KawaiiSpacing.md)
            ) {
                Text(text = stringResource(id = com.zcamstudio.kawaiipb.R.string.landing_admin_title), style = MaterialTheme.typography.headlineSmall, color = CherryPink)
                Text(
                    text = stringResource(id = com.zcamstudio.kawaiipb.R.string.landing_admin_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KawaiiPrimaryButton(text = stringResource(id = com.zcamstudio.kawaiipb.R.string.landing_admin_cancel), modifier = Modifier.weight(1f)) { onDismiss() }
                    KawaiiPrimaryButton(text = stringResource(id = com.zcamstudio.kawaiipb.R.string.landing_admin_unlock), modifier = Modifier.weight(1f)) { onConfirm() }
                }
            }
        }
    }
}
