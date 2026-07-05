package com.stillwater.app.ui.sos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.stillwater.app.data.TriggerRepository
import com.stillwater.app.data.UrgeRepository
import com.stillwater.app.data.UserValuesRepository
import com.stillwater.app.data.prefs.UserPreferencesRepository
import com.stillwater.app.domain.model.EntryPoint
import com.stillwater.app.domain.model.Mode
import com.stillwater.app.domain.model.MoodTag
import com.stillwater.app.domain.model.SosStep
import com.stillwater.app.domain.model.Trigger
import com.stillwater.app.domain.model.TriggerCategory
import com.stillwater.app.domain.model.UrgeOutcome
import com.stillwater.app.ui.navigation.SosRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The escalation ladder, plus the lapse path. One phase on screen at a time. */
enum class SosPhase { BREATHE, SURF, PLAN, RESOLVE, LOG, BLOOM, DEBRIEF, LAPSE_CLOSE }

const val BREATHE_SECONDS = 10
const val SURF_SECONDS = 90

/** (elapsed-seconds threshold, line) — the guided urge-surfing script. */
val SURF_SCRIPT = listOf(
    0 to "Find where the urge sits in your body.",
    15 to "It's a wave. Right now it's rising.",
    35 to "Waves peak. This one will too.",
    55 to "You don't have to act, or fight it. Just stay and watch.",
    75 to "It's already starting to fall.",
)

data class SosUiState(
    val phase: SosPhase = SosPhase.BREATHE,
    val surfElapsedSeconds: Int = 0,
    val values: List<String> = emptyList(),
    /** Profile mode; BOTH means the log asks which pull it was. */
    val profileMode: Mode? = null,
    val triggers: List<Trigger> = emptyList(),
    // Closing log (3 taps: feeling, spark, place — plus pull for BOTH users)
    val mood: MoodTag? = null,
    val sparkTriggerId: Long? = null,
    val placeTriggerId: Long? = null,
    val chosenMode: Mode? = null,
    // Lapse debrief
    val whatPreceded: String = "",
    val nextTimeIdea: String = "",
    val isSaving: Boolean = false,
) {
    val isLapsePath: Boolean
        get() = phase == SosPhase.DEBRIEF || phase == SosPhase.LAPSE_CLOSE

    val sparkTriggers: List<Trigger>
        get() = triggers.filter { it.category != TriggerCategory.PLACE }

    val placeTriggers: List<Trigger>
        get() = triggers.filter { it.category == TriggerCategory.PLACE }

    val needsModeChoice: Boolean get() = profileMode == Mode.BOTH

    val canSaveLog: Boolean
        get() = mood != null && sparkTriggerId != null && placeTriggerId != null &&
            (!needsModeChoice || chosenMode != null)

    val surfLine: String
        get() = SURF_SCRIPT.last { surfElapsedSeconds >= it.first }.second
}

@HiltViewModel
class SosViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val urgeRepository: UrgeRepository,
    private val triggerRepository: TriggerRepository,
    valuesRepository: UserValuesRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val entryPoint: EntryPoint =
        runCatching { EntryPoint.valueOf(savedStateHandle.toRoute<SosRoute>().entryPoint) }
            .getOrDefault(EntryPoint.IN_APP)

    private val _uiState = MutableStateFlow(
        SosUiState(phase = if (entryPoint == EntryPoint.RETRO_LOG) SosPhase.DEBRIEF else SosPhase.BREATHE),
    )
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    private var eventId: Long? = null
    /** Furthest ladder step reached; LOG once the closing log is shown. */
    private var furthestStep: SosStep = SosStep.BREATH
    private var timerJob: Job? = null
    private var resolved = false
    /** Went through the debrief → this episode resolves as LAPSED. */
    private var cameThroughDebrief = entryPoint == EntryPoint.RETRO_LOG

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.userPreferences.first()
            val values = valuesRepository.values.first().map { it.name }
            _uiState.update { it.copy(profileMode = prefs.mode, values = values) }
            eventId = urgeRepository.startEvent(entryPoint, prefs.mode)
        }
        viewModelScope.launch {
            triggerRepository.activeTriggers.collect { triggers ->
                _uiState.update { it.copy(triggers = triggers) }
            }
        }
        if (entryPoint != EntryPoint.RETRO_LOG) startBreatheTimer()
    }

    // ---- Ladder progression ----

    private fun startBreatheTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            delay(BREATHE_SECONDS * 1000L)
            advanceToSurf()
        }
    }

    fun advanceToSurf() {
        if (_uiState.value.phase != SosPhase.BREATHE) return
        furthestStep = SosStep.SURF
        _uiState.update { it.copy(phase = SosPhase.SURF) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.surfElapsedSeconds < SURF_SECONDS) {
                delay(1000)
                _uiState.update { it.copy(surfElapsedSeconds = it.surfElapsedSeconds + 1) }
            }
            advanceToPlan()
        }
    }

    fun advanceToPlan() {
        if (_uiState.value.phase != SosPhase.SURF) return
        timerJob?.cancel()
        furthestStep = SosStep.PLAN
        _uiState.update { it.copy(phase = SosPhase.PLAN) }
    }

    fun advanceToResolve() = _uiState.update { it.copy(phase = SosPhase.RESOLVE) }

    /** "It passed" → log → bloom. */
    fun wavePassed() {
        furthestStep = SosStep.LOG
        _uiState.update { it.copy(phase = SosPhase.LOG) }
    }

    /** "I acted on it" → the shame-free debrief, then the same log. */
    fun startLapsePath() {
        cameThroughDebrief = true
        _uiState.update { it.copy(phase = SosPhase.DEBRIEF) }
    }

    fun continueDebriefToLog() {
        furthestStep = SosStep.LOG
        _uiState.update { it.copy(phase = SosPhase.LOG) }
    }

    // ---- Log selections ----

    fun selectMood(mood: MoodTag) = _uiState.update { it.copy(mood = mood) }
    fun selectSpark(id: Long) = _uiState.update { it.copy(sparkTriggerId = id) }
    fun selectPlace(id: Long) = _uiState.update { it.copy(placeTriggerId = id) }
    fun selectEventMode(mode: Mode) = _uiState.update { it.copy(chosenMode = mode) }
    fun setWhatPreceded(text: String) = _uiState.update { it.copy(whatPreceded = text) }
    fun setNextTimeIdea(text: String) = _uiState.update { it.copy(nextTimeIdea = text) }

    fun saveLog() {
        val state = _uiState.value
        if (!state.canSaveLog || state.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val id = requireEventId()
            val outcome = if (cameThroughDebrief) UrgeOutcome.LAPSED else UrgeOutcome.SURFED
            urgeRepository.completeEvent(
                eventId = id,
                outcome = outcome,
                furthestStep = furthestStep,
                eventMode = state.chosenMode ?: state.profileMode,
                mood = state.mood,
                triggerIds = listOfNotNull(state.sparkTriggerId, state.placeTriggerId),
            )
            if (outcome == UrgeOutcome.LAPSED) {
                urgeRepository.attachDebrief(
                    eventId = id,
                    whatPreceded = state.whatPreceded,
                    nextTimeIdea = state.nextTimeIdea,
                    completed = true,
                )
            }
            resolved = true
            _uiState.update {
                it.copy(
                    isSaving = false,
                    phase = if (outcome == UrgeOutcome.LAPSED) SosPhase.LAPSE_CLOSE else SosPhase.BLOOM,
                )
            }
        }
    }

    /** Leaving early is allowed and never punished; the sweep records honesty. */
    fun onExitEarly() {
        if (resolved) return
        resolved = true
        val id = eventId ?: return
        viewModelScope.launch {
            urgeRepository.completeEvent(
                eventId = id,
                outcome = UrgeOutcome.ABANDONED,
                furthestStep = furthestStep,
                eventMode = _uiState.value.profileMode,
                mood = null,
            )
        }
    }

    private suspend fun requireEventId(): Long {
        // The insert races the UI only in pathological cases; wait briefly.
        repeat(50) {
            eventId?.let { return it }
            delay(20)
        }
        return eventId ?: error("Urge event was never created")
    }
}
