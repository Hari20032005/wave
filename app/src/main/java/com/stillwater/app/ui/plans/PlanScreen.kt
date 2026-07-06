package com.stillwater.app.ui.plans

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillwater.app.ui.components.CalmCard
import com.stillwater.app.ui.components.CalmPrimaryButton
import com.stillwater.app.ui.components.CalmQuietButton
import com.stillwater.app.ui.components.CalmTone
import com.stillwater.app.ui.sos.SosHaptics
import com.stillwater.app.ui.theme.Spacing

@Composable
fun PlanScreen(
    onCreatePlan: () -> Unit,
    onBack: () -> Unit,
    viewModel: PlanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = remember(context) { SosHaptics(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = Spacing.screenEdge),
        verticalArrangement = Arrangement.Center,
    ) {
        val plan = state.plan
        if (plan == null) {
            Text(
                text = "Decide once,\nnot in the moment.",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "An if-then plan links a risky moment to one small action, chosen " +
                    "while you're calm. Decisions made now don't have to be made under fire.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.xxl))
            CalmPrimaryButton(text = "Build my plan", onClick = onCreatePlan)
        } else {
            Text(
                text = "Your plan",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.lg))
            CalmCard(tone = CalmTone.Celebrate) {
                Text(
                    text = plan.sentence,
                    style = MaterialTheme.typography.displayLarge,
                )
            }
            Spacer(Modifier.height(Spacing.md))
            val timesWord = if (plan.rehearsalCount == 1) "time" else "times"
            Text(
                text = "Rehearsed ${plan.rehearsalCount} $timesWord · " +
                    "reminded quietly at your high-risk times",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xxl))
            CalmPrimaryButton(
                text = "Practice it once more",
                onClick = {
                    haptics.breathTurn()
                    viewModel.rehearse()
                },
            )
            Text(
                text = "More plans arrive with Premium, later.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = Spacing.md),
            )
        }
        CalmQuietButton(
            text = "Back",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
