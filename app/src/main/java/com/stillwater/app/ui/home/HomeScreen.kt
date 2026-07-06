package com.stillwater.app.ui.home

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillwater.app.ui.components.CalmCard
import com.stillwater.app.ui.components.CalmPrimaryButton
import com.stillwater.app.ui.components.CalmQuietButton
import com.stillwater.app.ui.theme.Spacing
import java.time.LocalTime

/**
 * Quiet by design: one clear action, no stats shouting, no streaks. The home
 * screen's whole job is being findable in a hard moment and forgettable
 * otherwise.
 */
@Composable
fun HomeScreen(
    onStartSos: () -> Unit,
    onLogSlip: () -> Unit = {},
    onSetupQuickAccess: () -> Unit = {},
    onOpenPlan: () -> Unit = {},
    onOpenProtection: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = Spacing.screenEdge),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = greetingForHour(LocalTime.now().hour),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "However the water is right now, you have a place to stand.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.huge))

        CalmPrimaryButton(
            text = "I'm feeling an urge",
            onClick = onStartSos,
            isCrisis = true,
        )
        CalmQuietButton(
            text = "I slipped earlier — log it",
            onClick = onLogSlip,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        CalmQuietButton(
            text = "My plan",
            onClick = onOpenPlan,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        CalmQuietButton(
            text = "App protection",
            onClick = onOpenProtection,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        if (!state.quickAccessEnabled) {
            Spacer(Modifier.height(Spacing.xxl))
            CalmCard {
                Text(
                    text = "Make it one tap",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Add the Stillwater widget to your home screen (long-press an " +
                        "empty spot → Widgets), or keep a silent shortcut in your " +
                        "notification shade.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
                CalmQuietButton(
                    text = "Notification shortcut",
                    onClick = onSetupQuickAccess,
                )
            }
        }
    }
}

private fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Morning."
    in 12..17 -> "Afternoon."
    in 18..22 -> "Evening."
    else -> "Still up."
}
