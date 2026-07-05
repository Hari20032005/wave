package com.stillwater.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stillwater.app.data.UserValuesRepository
import com.stillwater.app.data.prefs.UserPreferencesRepository
import com.stillwater.app.domain.model.Framing
import com.stillwater.app.domain.model.Mode
import com.stillwater.app.domain.model.MoodTag
import com.stillwater.app.domain.model.TriggerTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep {
    WELCOME, MODE, TRIGGER_TIMES, MOODS, VALUES, INCONGRUENCE, AFFIRMATION, PAYWALL
}

/** Mean agreement >= this on the moral-incongruence items routes to values framing. */
private const val VALUES_FRAMING_THRESHOLD = 3.5f

const val MAX_VALUES = 4
const val INCONGRUENCE_ITEM_COUNT = 3

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val mode: Mode? = null,
    val triggerTimes: Set<TriggerTime> = emptySet(),
    val moods: Set<MoodTag> = emptySet(),
    val selectedValues: List<String> = emptyList(),
    /** 1..5 per incongruence item; null = unanswered. */
    val incongruenceAnswers: List<Int?> = List(INCONGRUENCE_ITEM_COUNT) { null },
    val isFinishing: Boolean = false,
) {
    val framing: Framing
        get() {
            val answers = incongruenceAnswers.filterNotNull()
            if (answers.size < INCONGRUENCE_ITEM_COUNT) return Framing.HABIT_CHANGE
            return if (answers.average() >= VALUES_FRAMING_THRESHOLD) {
                Framing.VALUES_FIRST
            } else {
                Framing.HABIT_CHANGE
            }
        }

    /** The incongruence screen only applies when porn is in scope. */
    private val showsIncongruence: Boolean
        get() = mode == Mode.PORN || mode == Mode.BOTH

    val steps: List<OnboardingStep>
        get() = buildList {
            add(OnboardingStep.WELCOME)
            add(OnboardingStep.MODE)
            add(OnboardingStep.TRIGGER_TIMES)
            add(OnboardingStep.MOODS)
            add(OnboardingStep.VALUES)
            if (showsIncongruence) add(OnboardingStep.INCONGRUENCE)
            add(OnboardingStep.AFFIRMATION)
            add(OnboardingStep.PAYWALL)
        }

    val stepIndex: Int get() = steps.indexOf(step).coerceAtLeast(0)

    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.MODE -> mode != null
            OnboardingStep.TRIGGER_TIMES -> triggerTimes.isNotEmpty()
            OnboardingStep.MOODS -> moods.isNotEmpty()
            OnboardingStep.VALUES -> selectedValues.isNotEmpty()
            OnboardingStep.INCONGRUENCE -> incongruenceAnswers.all { it != null }
            else -> true
        }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val valuesRepository: UserValuesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun selectMode(mode: Mode) = _uiState.update { it.copy(mode = mode) }

    fun toggleTriggerTime(time: TriggerTime) = _uiState.update {
        it.copy(triggerTimes = it.triggerTimes.toggled(time))
    }

    fun toggleMood(mood: MoodTag) = _uiState.update {
        it.copy(moods = it.moods.toggled(mood))
    }

    fun toggleValue(name: String) = _uiState.update { state ->
        val current = state.selectedValues
        val next = when {
            name in current -> current - name
            current.size < MAX_VALUES -> current + name
            else -> current
        }
        state.copy(selectedValues = next)
    }

    fun answerIncongruence(index: Int, value: Int) = _uiState.update { state ->
        state.copy(
            incongruenceAnswers = state.incongruenceAnswers.toMutableList()
                .also { it[index] = value },
        )
    }

    fun next(onFinished: () -> Unit) {
        val state = _uiState.value
        if (!state.canContinue) return
        if (state.step == OnboardingStep.PAYWALL) {
            finish(onFinished)
            return
        }
        val steps = state.steps
        val nextStep = steps.getOrNull(state.stepIndex + 1) ?: return
        _uiState.update { it.copy(step = nextStep) }
    }

    fun back() {
        val state = _uiState.value
        val prev = state.steps.getOrNull(state.stepIndex - 1) ?: return
        _uiState.update { it.copy(step = prev) }
    }

    private fun finish(onFinished: () -> Unit) {
        val state = _uiState.value
        val mode = state.mode ?: return
        if (state.isFinishing) return
        _uiState.update { it.copy(isFinishing = true) }
        viewModelScope.launch {
            valuesRepository.replaceAll(state.selectedValues)
            preferencesRepository.completeOnboarding(
                mode = mode,
                triggerTimes = state.triggerTimes,
                commonMoods = state.moods,
                moralIncongruenceScore = state.incongruenceAnswers
                    .filterNotNull()
                    .takeIf { it.size == INCONGRUENCE_ITEM_COUNT }
                    ?.average()?.toFloat(),
                framing = state.framing,
            )
            onFinished()
        }
    }

    private fun <T> Set<T>.toggled(item: T): Set<T> =
        if (item in this) this - item else this + item
}
