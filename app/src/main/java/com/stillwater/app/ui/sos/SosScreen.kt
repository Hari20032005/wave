package com.stillwater.app.ui.sos

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillwater.app.domain.model.Mode
import com.stillwater.app.domain.model.MoodTag
import com.stillwater.app.domain.model.Trigger
import com.stillwater.app.ui.components.CalmCard
import com.stillwater.app.ui.components.CalmPrimaryButton
import com.stillwater.app.ui.components.CalmQuietButton
import com.stillwater.app.ui.components.CalmTone
import com.stillwater.app.ui.theme.Motion
import com.stillwater.app.ui.theme.Spacing
import com.stillwater.app.ui.theme.Tones
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * The escalation ladder. Crisis UX rules apply everywhere here: one action at
 * a time, huge targets, slow breathing-paced transitions, no decisions under
 * fire (Design Psychology #5).
 */
@Composable
fun SosScreen(
    onClose: () -> Unit,
    viewModel: SosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sosHaptics = remember(context) { SosHaptics(context) }

    // Leaving early is allowed and never punished — but it's recorded honestly.
    val exit = {
        viewModel.onExitEarly()
        onClose()
    }
    BackHandler { exit() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = Spacing.screenEdge),
    ) {
        AnimatedContent(
            targetState = state.phase,
            transitionSpec = {
                fadeIn(tween(Motion.DRIFT, easing = Motion.CalmEase)) togetherWith
                    fadeOut(tween(Motion.CALM, easing = Motion.CalmEase))
            },
            label = "sosPhase",
            modifier = Modifier.weight(1f),
        ) { phase ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Spacing.lg),
                verticalArrangement = Arrangement.Center,
            ) {
                when (phase) {
                    SosPhase.BREATHE -> BreathePhase(sosHaptics, onSkip = viewModel::advanceToSurf)
                    SosPhase.SURF -> SurfPhase(
                        elapsedSeconds = state.surfElapsedSeconds,
                        line = state.surfLine,
                        personalEvidence = state.personalEvidence,
                        haptics = sosHaptics,
                        onSkip = viewModel::advanceToPlan,
                    )
                    SosPhase.PLAN -> PlanPhase(
                        values = state.values,
                        planSentence = state.plan?.sentence,
                        onContinue = viewModel::advanceToResolve,
                    )
                    SosPhase.RESOLVE -> ResolvePhase(
                        onPassed = viewModel::wavePassed,
                        onLapsed = viewModel::startLapsePath,
                    )
                    SosPhase.LOG -> LogPhase(
                        state = state,
                        onMood = viewModel::selectMood,
                        onSpark = viewModel::selectSpark,
                        onPlace = viewModel::selectPlace,
                        onMode = viewModel::selectEventMode,
                        onSave = viewModel::saveLog,
                    )
                    SosPhase.BLOOM -> BloomPhase(
                        topValue = state.values.firstOrNull(),
                        haptics = sosHaptics,
                        onDone = onClose,
                    )
                    SosPhase.DEBRIEF -> DebriefPhase(
                        whatPreceded = state.whatPreceded,
                        nextTimeIdea = state.nextTimeIdea,
                        onWhatPreceded = viewModel::setWhatPreceded,
                        onNextTimeIdea = viewModel::setNextTimeIdea,
                        onContinue = viewModel::continueDebriefToLog,
                    )
                    SosPhase.LAPSE_CLOSE -> LapseClosePhase(onDone = onClose)
                }
            }
        }
    }
}

// ---- Phase 1: 10-second breathing pause ----

@Composable
private fun BreathePhase(haptics: SosHaptics, onSkip: () -> Unit) {
    var caption by remember { mutableStateOf("Breathe in…") }
    val breath = rememberInfiniteTransition(label = "breath")
    val scale by breath.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = Motion.BreathEase),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathScale",
    )

    LaunchedEffect(Unit) {
        while (true) {
            caption = "Breathe in…"
            delay(4000)
            haptics.breathTurn()
            caption = "…and slowly out."
            delay(4000)
            haptics.breathTurn()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .size(220.dp)
                    .scale(scale),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {}
            Text(
                text = caption,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(Spacing.xxl))
        CalmQuietButton(text = "Skip the pause", onClick = onSkip)
    }
}

// ---- Phase 2: 90-second urge surfing ----

@Composable
private fun SurfPhase(
    elapsedSeconds: Int,
    line: String,
    personalEvidence: String?,
    haptics: SosHaptics,
    onSkip: () -> Unit,
) {
    // A soft pulse roughly every 10s keeps the body anchored without alarm.
    LaunchedEffect(elapsedSeconds / 10) {
        if (elapsedSeconds > 0) haptics.surfPulse()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = line,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (personalEvidence != null) {
            Spacer(Modifier.height(Spacing.md))
            // Their own track record — the one line a craving brain believes.
            Text(
                text = personalEvidence,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.xxl))
        WaveCanvas(elapsedSeconds = elapsedSeconds)
        Spacer(Modifier.height(Spacing.xxl))
        CalmQuietButton(text = "I'm steady — keep going", onClick = onSkip)
    }
}

/** The urge as a literal wave: amplitude rises, peaks mid-way, and falls. */
@Composable
private fun WaveCanvas(elapsedSeconds: Int) {
    val drift = rememberInfiniteTransition(label = "waveDrift")
    val phaseShift by drift.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "wavePhase",
    )
    val waveColor = MaterialTheme.colorScheme.primary

    // Amplitude follows the urge arc across the 90s: rise → peak → fall.
    val arc = sin(PI * (elapsedSeconds.coerceIn(0, SURF_SECONDS) / SURF_SECONDS.toFloat()))
        .toFloat()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
    ) {
        val midY = size.height / 2
        val amplitude = (8f + 40f * arc) * density
        val path = Path()
        path.moveTo(0f, midY)
        var x = 0f
        while (x <= size.width) {
            val y = midY + amplitude * sin((x / size.width) * 4 * PI.toFloat() + phaseShift)
            path.lineTo(x, y)
            x += 6f
        }
        drawPath(
            path = path,
            color = waveColor.copy(alpha = 0.55f),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

// ---- Phase 3: their own map, reflected back ----

@Composable
private fun PlanPhase(values: List<String>, planSentence: String?, onContinue: () -> Unit) {
    Text(
        text = if (planSentence != null) "You already decided this." else "Remember what this is for.",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.lg))
    if (planSentence != null) {
        CalmCard(tone = CalmTone.Celebrate) {
            Text(text = "Your plan", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(Spacing.sm))
            Text(text = planSentence, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Spacing.md))
    }
    CalmCard {
        Text(text = "You chose to protect:", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(Spacing.sm))
        values.forEach { value ->
            Text(text = value, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.xs))
        }
        if (values.isEmpty()) {
            Text(
                text = "Your reasons will live here once you set them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (planSentence == null) {
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "Your if-then plan will appear here once you build one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(Spacing.xxl))
    CalmPrimaryButton(text = "Continue", onClick = onContinue, isCrisis = true)
}

// ---- Phase 4: resolution — both paths carry equal dignity ----

@Composable
private fun ResolvePhase(onPassed: () -> Unit, onLapsed: () -> Unit) {
    Text(
        text = "How is the wave now?",
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.xxl))
    CalmPrimaryButton(text = "It passed", onClick = onPassed, isCrisis = true)
    Spacer(Modifier.height(Spacing.md))
    LapseToneButton(text = "I acted on it", onClick = onLapsed)
}

/** Lapse-tone button: warm sand, full-size — never small, never red. */
@Composable
private fun LapseToneButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Spacing.sosTouchTarget),
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = Tones.current.lapseContainer,
            contentColor = Tones.current.onLapseContainer,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

// ---- Phase 5: the three-tap log ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogPhase(
    state: SosUiState,
    onMood: (MoodTag) -> Unit,
    onSpark: (Long) -> Unit,
    onPlace: (Long) -> Unit,
    onMode: (Mode) -> Unit,
    onSave: () -> Unit,
) {
    Text(
        text = if (state.isLapsePath) "A little context — for your map." else "Three taps, then rest.",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.lg))

    LogSection("How do you feel right now?") {
        moodChips.forEach { (mood, label) ->
            CalmChip(label, state.mood == mood) { onMood(mood) }
        }
    }
    LogSection("What set it off?") {
        state.sparkTriggers.forEach { trigger ->
            CalmChip(trigger.name, state.sparkTriggerId == trigger.id) { onSpark(trigger.id) }
        }
    }
    LogSection("Where are you?") {
        state.placeTriggers.forEach { trigger ->
            CalmChip(trigger.name, state.placeTriggerId == trigger.id) { onPlace(trigger.id) }
        }
    }
    if (state.needsModeChoice) {
        LogSection("Which pull was it?") {
            CalmChip("Social media", state.chosenMode == Mode.SOCIAL) { onMode(Mode.SOCIAL) }
            CalmChip("Pornography", state.chosenMode == Mode.PORN) { onMode(Mode.PORN) }
        }
    }

    Spacer(Modifier.height(Spacing.lg))
    CalmPrimaryButton(
        text = "Save",
        onClick = onSave,
        enabled = state.canSaveLog && !state.isSaving,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogSection(title: String, chips: @Composable () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.sm))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        chips()
    }
    Spacer(Modifier.height(Spacing.lg))
}

private val moodChips = listOf(
    MoodTag.STRESSED to "Stressed",
    MoodTag.BORED to "Bored",
    MoodTag.LONELY to "Lonely",
    MoodTag.SAD to "Sad",
    MoodTag.ANXIOUS to "Anxious",
    MoodTag.TIRED to "Tired",
)

@Composable
private fun CalmChip(label: String, selected: Boolean, onClick: () -> Unit) {
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

// ---- Phase 6a: the calm celebration (soft bloom, never confetti) ----

@Composable
private fun BloomPhase(topValue: String?, haptics: SosHaptics, onDone: () -> Unit) {
    val bloomScale = remember { Animatable(0.3f) }
    val bloomAlpha = remember { Animatable(0.35f) }
    LaunchedEffect(Unit) {
        haptics.bloom()
        bloomScale.animateTo(2.2f, tween(Motion.DRIFT * 2, easing = Motion.CalmEase))
    }
    LaunchedEffect(Unit) {
        bloomAlpha.animateTo(0f, tween(Motion.DRIFT * 2, easing = Motion.CalmEase))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .size(160.dp)
                    .scale(bloomScale.value)
                    .alpha(bloomAlpha.value),
                shape = CircleShape,
                color = Tones.current.celebrateContainer,
            ) {}
            Text(
                text = "The wave passed.",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = "You stayed. That's the whole skill.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (topValue != null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Still here for: $topValue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.xxl))
        CalmPrimaryButton(text = "Done", onClick = onDone)
    }
}

// ---- Lapse path: the shame-free debrief ----

@Composable
private fun DebriefPhase(
    whatPreceded: String,
    nextTimeIdea: String,
    onWhatPreceded: (String) -> Unit,
    onNextTimeIdea: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Text(
        text = "Okay. You're still here.",
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.md))
    Text(
        text = "A slip is information, not a verdict. Two minutes here turns it into " +
            "something useful. Both questions are optional.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(Spacing.lg))
    DebriefField(
        label = "What was happening just before?",
        value = whatPreceded,
        onValueChange = onWhatPreceded,
    )
    Spacer(Modifier.height(Spacing.md))
    DebriefField(
        label = "What might you try next time?",
        value = nextTimeIdea,
        onValueChange = onNextTimeIdea,
    )
    Spacer(Modifier.height(Spacing.xl))
    CalmPrimaryButton(text = "Continue", onClick = onContinue)
}

@Composable
private fun DebriefField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}

@Composable
private fun LapseClosePhase(onDone: () -> Unit) {
    CalmCard(tone = CalmTone.Lapse) {
        Text(text = "Logged — for your map.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "Not against you. What matters now is how gently and how quickly " +
                "you come back — and you're already back.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    Spacer(Modifier.height(Spacing.xxl))
    CalmPrimaryButton(text = "Done", onClick = onDone)
}
