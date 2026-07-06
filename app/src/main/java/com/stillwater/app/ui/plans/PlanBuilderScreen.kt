package com.stillwater.app.ui.plans

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillwater.app.ui.components.CalmCard
import com.stillwater.app.ui.components.CalmPrimaryButton
import com.stillwater.app.ui.components.CalmQuietButton
import com.stillwater.app.ui.components.CalmTone
import com.stillwater.app.ui.sos.SosHaptics
import com.stillwater.app.ui.theme.Motion
import com.stillwater.app.ui.theme.Spacing
import kotlinx.coroutines.delay

/**
 * Guided implementation-intention builder. The output is always the strict
 * "If [situation], then I will [action]" sentence, and the rehearsal step is
 * mandatory before saving — rehearsed contingent plans are the ones that fire.
 */
@Composable
fun PlanBuilderScreen(
    onFinished: () -> Unit,
    viewModel: PlanBuilderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler { if (!viewModel.back()) onFinished() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = Spacing.screenEdge),
    ) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                fadeIn(tween(Motion.CALM, easing = Motion.CalmEase)) togetherWith
                    fadeOut(tween(Motion.CALM, easing = Motion.CalmEase))
            },
            label = "builderStep",
            modifier = Modifier.weight(1f),
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Spacing.lg),
                verticalArrangement = Arrangement.Center,
            ) {
                when (step) {
                    BuilderStep.SITUATION -> SituationStep(state, viewModel)
                    BuilderStep.ACTION -> ActionStep(state, viewModel)
                    BuilderStep.PREVIEW -> PreviewStep(state)
                    BuilderStep.REHEARSAL -> RehearsalStep(state, viewModel)
                }
            }
        }

        CalmPrimaryButton(
            text = when (state.step) {
                BuilderStep.REHEARSAL -> "Save my plan"
                else -> "Continue"
            },
            onClick = { viewModel.next(onFinished) },
            enabled = state.canContinue && !state.isSaving,
        )
        CalmQuietButton(
            text = if (state.step == BuilderStep.SITUATION) "Cancel" else "Back",
            onClick = { if (!viewModel.back()) onFinished() },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(Spacing.sm))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SituationStep(state: PlanBuilderUiState, viewModel: PlanBuilderViewModel) {
    Text(
        text = "If…",
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.sm))
    Text(
        text = "Pick the moment this plan is for — the more specific, the better it works.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.lg))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        state.situationTriggers.forEach { trigger ->
            BuilderChip(
                label = trigger.name,
                selected = state.selectedTriggerId == trigger.id,
                onClick = { viewModel.selectTrigger(trigger.id) },
            )
        }
    }
    Spacer(Modifier.height(Spacing.lg))
    OutlinedTextField(
        value = state.customSituation,
        onValueChange = viewModel::setCustomSituation,
        label = { Text("Or describe it your way", style = MaterialTheme.typography.bodyMedium) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionStep(state: PlanBuilderUiState, viewModel: PlanBuilderViewModel) {
    Text(
        text = "…then I will:",
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.sm))
    Text(
        text = "One small, doable action. Something your hands can start in ten seconds.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.lg))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        actionSuggestions.forEach { action ->
            BuilderChip(
                label = action,
                selected = state.selectedAction == action,
                onClick = { viewModel.selectAction(action) },
            )
        }
    }
    Spacer(Modifier.height(Spacing.lg))
    OutlinedTextField(
        value = state.customAction,
        onValueChange = viewModel::setCustomAction,
        label = { Text("Or your own action", style = MaterialTheme.typography.bodyMedium) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}

@Composable
private fun PreviewStep(state: PlanBuilderUiState) {
    Text(
        text = "Your plan.",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.lg))
    CalmCard(tone = CalmTone.Celebrate) {
        Text(
            text = state.sentence,
            style = MaterialTheme.typography.displayLarge,
        )
    }
    Spacer(Modifier.height(Spacing.md))
    Text(
        text = "It will be there in the SOS flow, and Stillwater will remind you of it " +
            "quietly at your high-risk times.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RehearsalStep(state: PlanBuilderUiState, viewModel: PlanBuilderViewModel) {
    val context = LocalContext.current
    val haptics = remember(context) { SosHaptics(context) }

    // A slow pulse paces the visualization; after REHEARSAL_SECONDS the
    // rehearsal is considered done and the save button unlocks.
    val pulse = rememberInfiniteTransition(label = "rehearsePulse")
    val scale by pulse.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = Motion.BreathEase),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rehearseScale",
    )
    LaunchedEffect(Unit) {
        repeat(REHEARSAL_SECONDS / 5) {
            delay(5000)
            haptics.breathTurn()
        }
        viewModel.markRehearsed()
    }

    Text(
        text = "Now live it once.",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.md))
    Text(
        text = "Close your eyes. Picture the moment — where you are, what you feel. " +
            "Now watch yourself do it:",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(Spacing.lg))
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .size(180.dp)
                .scale(scale),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {}
    }
    Spacer(Modifier.height(Spacing.lg))
    Text(
        text = state.sentence,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    if (!state.rehearsed) {
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "Take your time — a few slow breaths.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BuilderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        shape = MaterialTheme.shapes.extraLarge,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier.defaultMinSize(minHeight = Spacing.minTouchTarget),
    )
}
