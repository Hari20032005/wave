package com.stillwater.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillwater.app.domain.model.Framing
import com.stillwater.app.domain.model.Mode
import com.stillwater.app.domain.model.MoodTag
import com.stillwater.app.domain.model.TriggerTime
import com.stillwater.app.ui.components.CalmCard
import com.stillwater.app.ui.components.CalmPrimaryButton
import com.stillwater.app.ui.components.CalmQuietButton
import com.stillwater.app.ui.components.CalmTone
import com.stillwater.app.ui.theme.Motion
import com.stillwater.app.ui.theme.Spacing

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.stepIndex > 0) { viewModel.back() }

    val progress by animateFloatAsState(
        targetValue = state.stepIndex / (state.steps.size - 1).toFloat(),
        animationSpec = tween(Motion.CALM, easing = Motion.CalmEase),
        label = "onboardingProgress",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = Spacing.screenEdge),
    ) {
        Spacer(Modifier.height(Spacing.md))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            drawStopIndicator = {},
        )

        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                fadeIn(tween(Motion.CALM, easing = Motion.CalmEase)) togetherWith
                    fadeOut(tween(Motion.CALM, easing = Motion.CalmEase))
            },
            label = "onboardingStep",
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
                    OnboardingStep.WELCOME -> WelcomeStep()
                    OnboardingStep.MODE -> ModeStep(state.mode, viewModel::selectMode)
                    OnboardingStep.TRIGGER_TIMES ->
                        TriggerTimesStep(state.triggerTimes, viewModel::toggleTriggerTime)
                    OnboardingStep.MOODS -> MoodsStep(state.moods, viewModel::toggleMood)
                    OnboardingStep.VALUES -> ValuesStep(state.selectedValues, viewModel::toggleValue)
                    OnboardingStep.INCONGRUENCE ->
                        IncongruenceStep(state.incongruenceAnswers, viewModel::answerIncongruence)
                    OnboardingStep.AFFIRMATION ->
                        AffirmationStep(state.selectedValues, state.framing)
                    OnboardingStep.PAYWALL -> PaywallPlaceholderStep()
                }
            }
        }

        CalmPrimaryButton(
            text = when (state.step) {
                OnboardingStep.WELCOME -> "Begin"
                OnboardingStep.PAYWALL -> "Start using Stillwater"
                else -> "Continue"
            },
            onClick = { viewModel.next(onFinished) },
            enabled = state.canContinue && !state.isFinishing,
        )
        if (state.stepIndex > 0) {
            CalmQuietButton(
                text = "Back",
                onClick = viewModel::back,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        } else {
            Spacer(Modifier.height(Spacing.minTouchTarget))
        }
        Spacer(Modifier.height(Spacing.sm))
    }
}

// ---- Steps ----

@Composable
private fun StepHeader(title: String, subtitle: String? = null) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    if (subtitle != null) {
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(Spacing.lg))
}

@Composable
private fun WelcomeStep() {
    Text(
        text = "Stillwater",
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.md))
    Text(
        text = "A quiet place to change your relationship with the things that pull at you.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(Spacing.lg))
    CalmCard {
        Text(
            text = "Private by design",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "Everything you share stays on this phone. No account, no cloud, " +
                "no analytics. We can't leak what we never collect.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModeStep(selected: Mode?, onSelect: (Mode) -> Unit) {
    StepHeader(
        title = "What would you like to work on?",
        subtitle = "You can change this later.",
    )
    ModeOption(Mode.SOCIAL, "Social media", "Feeds and apps that take more than they give", Icons.Outlined.Smartphone, selected, onSelect)
    Spacer(Modifier.height(Spacing.md))
    ModeOption(Mode.PORN, "Pornography", "A habit that doesn't fit who you want to be", Icons.Outlined.Shield, selected, onSelect)
    Spacer(Modifier.height(Spacing.md))
    ModeOption(Mode.BOTH, "Both", "They often feed each other", Icons.Outlined.AllInclusive, selected, onSelect)
}

@Composable
private fun ModeOption(
    mode: Mode,
    title: String,
    description: String,
    icon: ImageVector,
    selected: Mode?,
    onSelect: (Mode) -> Unit,
) {
    OptionCard(
        title = title,
        description = description,
        icon = icon,
        selected = selected == mode,
        onClick = { onSelect(mode) },
    )
}

private val triggerTimeOptions = listOf(
    Triple(TriggerTime.LATE_NIGHT, "Late at night", Icons.Outlined.DarkMode),
    Triple(TriggerTime.MORNING, "Mornings", Icons.Outlined.WbTwilight),
    Triple(TriggerTime.AFTER_WORK, "After work or class", Icons.Outlined.Work),
    Triple(TriggerTime.WEEKEND, "Weekends", Icons.Outlined.Weekend),
    Triple(TriggerTime.WHEN_ALONE, "When I'm alone", Icons.Outlined.Person),
    Triple(TriggerTime.UNPREDICTABLE, "It's unpredictable", Icons.Outlined.Shuffle),
)

@Composable
private fun TriggerTimesStep(selected: Set<TriggerTime>, onToggle: (TriggerTime) -> Unit) {
    StepHeader(
        title = "When does the pull usually come?",
        subtitle = "Choose all that fit — this shapes when Stillwater checks in.",
    )
    triggerTimeOptions.forEach { (time, label, icon) ->
        OptionCard(
            title = label,
            icon = icon,
            selected = time in selected,
            onClick = { onToggle(time) },
            multiSelect = true,
        )
        Spacer(Modifier.height(Spacing.sm))
    }
}

private val moodOptions = listOf(
    MoodTag.STRESSED to "Stress",
    MoodTag.BORED to "Boredom",
    MoodTag.LONELY to "Loneliness",
    MoodTag.SAD to "Sadness",
    MoodTag.ANXIOUS to "Anxiety",
    MoodTag.TIRED to "Tiredness",
)

@Composable
private fun MoodsStep(selected: Set<MoodTag>, onToggle: (MoodTag) -> Unit) {
    StepHeader(
        title = "What's usually there just before?",
        subtitle = "Urges often ride in on a feeling. Choose any that sound familiar.",
    )
    moodOptions.forEach { (mood, label) ->
        OptionCard(
            title = label,
            selected = mood in selected,
            onClick = { onToggle(mood) },
            multiSelect = true,
        )
        Spacer(Modifier.height(Spacing.sm))
    }
}

/** Curated, values-assessment style list — tappable, never free text. */
val valueOptions = listOf(
    "Being present with people I love",
    "Deep focus and real work",
    "Genuine connection",
    "Self-respect",
    "My faith or spiritual life",
    "Health and energy",
    "Creating things",
    "Honesty with myself",
)

@Composable
private fun ValuesStep(selected: List<String>, onToggle: (String) -> Unit) {
    StepHeader(
        title = "What are you protecting?",
        subtitle = "Pick up to $MAX_VALUES. Stillwater will reflect these back when it matters most.",
    )
    valueOptions.forEach { value ->
        OptionCard(
            title = value,
            selected = value in selected,
            onClick = { onToggle(value) },
            multiSelect = true,
        )
        Spacer(Modifier.height(Spacing.sm))
    }
}

/**
 * Moral-incongruence screening (shown for porn/both modes). High agreement
 * routes to values/acceptance framing instead of abstinence framing — for many
 * people the distress is values-conflict, not loss of control.
 */
val incongruenceStatements = listOf(
    "Using pornography goes against my values or beliefs.",
    "I believe watching pornography is wrong.",
    "It troubles my conscience even when it doesn't disrupt my day-to-day life.",
)

@Composable
private fun IncongruenceStep(answers: List<Int?>, onAnswer: (Int, Int) -> Unit) {
    StepHeader(
        title = "Two more minutes",
        subtitle = "How much does each of these sound like you? There are no wrong answers — " +
            "this changes how the app talks with you.",
    )
    incongruenceStatements.forEachIndexed { index, statement ->
        AgreementRow(
            statement = statement,
            value = answers.getOrNull(index),
            onValueChange = { onAnswer(index, it) },
        )
        Spacer(Modifier.height(Spacing.lg))
    }
}

/** The peak/end moment: their own values, reflected back. */
@Composable
private fun AffirmationStep(values: List<String>, framing: Framing) {
    Text(
        text = "You've drawn your map.",
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.lg))
    CalmCard(tone = CalmTone.Celebrate) {
        Text(
            text = "You're doing this for:",
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(Spacing.sm))
        values.forEach { value ->
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(Spacing.xs))
        }
    }
    Spacer(Modifier.height(Spacing.lg))
    Text(
        text = when (framing) {
            Framing.VALUES_FIRST ->
                "This won't be about counting clean days. It's about living closer to what " +
                    "you just named — and being kind to yourself along the way."
            Framing.HABIT_CHANGE ->
                "This is a skill, not a test of willpower: noticing an urge as it rises, " +
                    "and letting it pass. Every wave you ride makes the next one smaller."
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun PaywallPlaceholderStep() {
    StepHeader(title = "Everything is free right now")
    CalmCard {
        Text(
            text = "Stillwater is being built in the open. Later, a few advanced tools — " +
                "app interception, insights — will become part of a paid tier. " +
                "The SOS flow, logging, and your plan will always be free.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
