package com.stillwater.app.ui.home

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.stillwater.app.ui.components.CalmCard
import com.stillwater.app.ui.components.CalmPrimaryButton
import com.stillwater.app.ui.components.CalmQuietButton
import com.stillwater.app.ui.theme.Spacing

/**
 * Prominent disclosure BEFORE the system notification-permission prompt
 * (Principle 5): full screen, plain language, no dark patterns, an equally
 * easy "not now".
 */
@Composable
fun NotificationDisclosureScreen(
    onDone: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.enableQuickAccess()
        onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = Spacing.screenEdge),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "A quiet shortcut",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.lg))
        CalmCard {
            Text(
                text = "What this does",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Keeps one silent notification in your shade, so the SOS flow is " +
                    "one tap away when a wave rises.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "What it doesn't do",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "No sounds, no reminders, no \"come back\" nudges. Stillwater never " +
                    "reads your other notifications, and it's hidden on your lock screen. " +
                    "Nothing leaves this phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.xxl))
        CalmPrimaryButton(
            text = "Turn it on",
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.enableQuickAccess()
                    onDone()
                }
            },
        )
        CalmQuietButton(
            text = "Not now",
            onClick = onDone,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
