package com.stillwater.app.ui.protection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillwater.app.ui.components.CalmCard
import com.stillwater.app.ui.components.CalmQuietButton
import com.stillwater.app.ui.theme.Spacing

/**
 * Setup and status for the interception layer. Each sensitive grant gets a
 * plain-language disclosure ON this screen before we hand off to system
 * settings — no dark patterns, and "not now" is always fine (Principle 5).
 */
@Composable
fun ProtectionScreen(
    onPickApps: () -> Unit,
    onOpenPaywall: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProtectionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-read grants whenever the user comes back from system settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenEdge, vertical = Spacing.lg),
    ) {
        Text(
            text = "Meet me at the door",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "When you open an app you've chosen, during hours you've chosen, " +
                "Stillwater steps in first with a calm pause. Only app names are ever " +
                "seen — no content, no messages, nothing leaves the phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))

        if (!state.isPremium) {
            // Premium gate — everything below stays visible so the user can
            // see exactly what they'd be getting, but setup starts here.
            CalmCard {
                Text("Part of Premium", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "App protection is a Premium tool. The SOS flow, logging, " +
                        "and your plan stay free forever.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
                CalmQuietButton(text = "See Premium", onClick = onOpenPaywall)
            }
            Spacer(Modifier.height(Spacing.lg))
            CalmQuietButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            return@Column
        }

        // Master toggle
        CalmCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Protection", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = when {
                            state.interceptionEnabled -> "On — watching your chosen apps"
                            !state.permissionsReady -> "Needs the two permissions below"
                            state.monitoredCount == 0 -> "Choose at least one app below"
                            else -> "Off"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(Spacing.md))
                Switch(
                    checked = state.interceptionEnabled,
                    onCheckedChange = { viewModel.setEnabled(it) },
                    enabled = state.canEnable || state.interceptionEnabled,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))

        PermissionCard(
            title = "See which app is in front",
            granted = state.hasUsageAccess,
            body = "Android calls this \"usage access.\" Stillwater reads only the name " +
                "of the app currently on screen, about once a second, and only while " +
                "protection is on. It never reads what's inside any app.",
            actionText = "Open usage access settings",
            onAction = { context.startActivity(viewModel.usageAccessIntent()) },
        )
        PermissionCard(
            title = "Draw the calm pause over other apps",
            granted = state.hasOverlay,
            body = "\"Display over other apps\" lets the pause screen appear on top of " +
                "the app you're opening. It shows only Stillwater's own screen.",
            actionText = "Open overlay settings",
            onAction = { context.startActivity(viewModel.overlayIntent()) },
        )
        PermissionCard(
            title = "Stay awake in the background (optional)",
            granted = state.hasBatteryExemption,
            body = "Some phones put Stillwater to sleep to save battery, which silently " +
                "turns protection off. This exemption keeps it reliable.",
            actionText = "Request battery exemption",
            onAction = { runCatching { context.startActivity(viewModel.batteryExemptionIntent()) } },
        )

        Spacer(Modifier.height(Spacing.md))
        CalmCard {
            Text("Protected apps", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = if (state.monitoredCount == 0) {
                    "None chosen yet."
                } else {
                    "${state.monitoredCount} app${if (state.monitoredCount == 1) "" else "s"} protected."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
            CalmQuietButton(text = "Choose apps", onClick = onPickApps)
        }

        Spacer(Modifier.height(Spacing.md))
        CalmCard {
            Text("When it watches", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = if (state.windowLabels.isEmpty()) {
                    "All day, every day — no risk windows are set, so protection is " +
                        "always on while enabled."
                } else {
                    "During your risk windows: ${state.windowLabels.joinToString(", ")}."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val oemIntents = viewModel.oemIntents()
        if (oemIntents.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.md))
            CalmCard {
                Text(
                    "${viewModel.oemName()} phones need one more step",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "${viewModel.oemName()}'s battery manager can quietly kill " +
                        "background apps. Allow Stillwater to auto-start / run in the " +
                        "background in the screen that opens. If nothing opens, find " +
                        "Stillwater under Settings → Battery and allow background activity.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
                CalmQuietButton(
                    text = "Open ${viewModel.oemName()} settings",
                    onClick = {
                        oemIntents.firstOrNull { intent ->
                            runCatching { context.startActivity(intent) }.isSuccess
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        CalmQuietButton(
            text = "Back",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    granted: Boolean,
    body: String,
    actionText: String,
    onAction: () -> Unit,
) {
    CalmCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (granted) {
                Text(
                    text = "Granted",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!granted) {
            Spacer(Modifier.height(Spacing.sm))
            CalmQuietButton(text = actionText, onClick = onAction)
        }
    }
    Spacer(Modifier.height(Spacing.md))
}
