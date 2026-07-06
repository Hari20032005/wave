package com.stillwater.app.ui.paywall

import android.app.Activity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillwater.app.ui.components.CalmCard
import com.stillwater.app.ui.components.CalmPrimaryButton
import com.stillwater.app.ui.components.CalmQuietButton
import com.stillwater.app.ui.theme.Spacing

/**
 * The paywall — calm and honest: no countdown timers, no fake discounts, no
 * guilt copy, and leaving is always one quiet tap. Annual pre-selected.
 */
@Composable
fun PaywallScreen(
    onClose: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenEdge, vertical = Spacing.lg),
    ) {
        PaywallContent(
            viewModel = viewModel,
            continueText = "Not now",
            onContinueFree = onClose,
        )
    }
}

@Composable
fun PaywallContent(
    continueText: String?,
    onContinueFree: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    Text(
        text = "Stillwater Premium",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.sm))
    Text(
        text = "The SOS flow, logging, and one plan are free forever. " +
            "Premium adds the tools that stand watch with you:",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.md))
    CalmCard {
        FeatureLine("App protection — a calm pause at the door of risky apps")
        FeatureLine("Insights — patterns across your triggers and hours")
        FeatureLine("Unlimited if-then plans")
    }
    Spacer(Modifier.height(Spacing.lg))

    if (state.isPremium) {
        CalmCard {
            Text(
                text = "You have Premium. Thank you for supporting Stillwater.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    } else if (!state.billingAvailable) {
        CalmCard {
            Text(
                text = "Google Play billing isn't available on this device right now. " +
                    "Everything free stays free — try again another time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        state.options.forEach { option ->
            PlanCard(
                option = option,
                selected = state.selected == option.choice,
                onSelect = { viewModel.select(option.choice) },
            )
            Spacer(Modifier.height(Spacing.sm))
        }
        Spacer(Modifier.height(Spacing.md))
        CalmPrimaryButton(
            text = state.selectedOption?.trialText?.let { "Start my $it" } ?: "Continue",
            onClick = { activity?.let(viewModel::purchase) },
            enabled = state.selectedOption != null,
        )
        CalmQuietButton(
            text = "Restore purchases",
            onClick = viewModel::restore,
        )
    }

    if (continueText != null) {
        Spacer(Modifier.height(Spacing.sm))
        CalmQuietButton(text = continueText, onClick = onContinueFree)
    }
}

@Composable
private fun FeatureLine(text: String) {
    Text(
        text = "·  $text",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(vertical = Spacing.xs),
    )
}

@Composable
private fun PlanCard(option: PlanOption, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                shape = MaterialTheme.shapes.large,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (option.choice == PlanChoice.ANNUAL) "Annual" else "Monthly",
                    style = MaterialTheme.typography.titleMedium,
                )
                option.trialText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = option.priceText, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = option.periodText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
