package com.stillwater.app.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stillwater.app.data.PlanRepository
import com.stillwater.app.data.RiskWindowRepository
import com.stillwater.app.data.TriggerRepository
import com.stillwater.app.domain.model.Trigger
import com.stillwater.app.domain.model.TriggerCategory
import com.stillwater.app.work.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BuilderStep { SITUATION, ACTION, PREVIEW, REHEARSAL }

/** Rehearsal research: mentally simulating the situation→action link is what
 *  makes implementation intentions fire automatically later. */
const val REHEARSAL_SECONDS = 15

/**
 * Seed-trigger labels are log chips ("Couldn't sleep"), not sentence clauses.
 * This maps them to first-person situations so the plan reads naturally:
 * "If I can't sleep, then I will…". Keyed by the fixed seed ids.
 */
val situationPhrases = mapOf(
    1L to "I'm scrolling a feed",
    2L to "I've just opened my phone",
    3L to "I see something triggering",
    4L to "I'm putting something off",
    5L to "I can't sleep",
    6L to "I've just had an argument",
)

val actionSuggestions = listOf(
    "open Stillwater and surf the wave",
    "put my phone in another room",
    "step outside for two minutes",
    "take ten slow breaths",
    "message someone I trust",
    "get a glass of water first",
)

data class PlanBuilderUiState(
    val step: BuilderStep = BuilderStep.SITUATION,
    val situationTriggers: List<Trigger> = emptyList(),
    val selectedTriggerId: Long? = null,
    val customSituation: String = "",
    val selectedAction: String? = null,
    val customAction: String = "",
    val rehearsed: Boolean = false,
    val isSaving: Boolean = false,
) {
    val situationText: String
        get() = customSituation.trim().ifEmpty {
            val trigger = situationTriggers.firstOrNull { it.id == selectedTriggerId } ?: return@ifEmpty ""
            situationPhrases[trigger.id] ?: trigger.name.lowercase()
        }

    val actionText: String
        get() = customAction.trim().ifEmpty { selectedAction ?: "" }

    val sentence: String get() = "If $situationText, then I will $actionText."

    val canContinue: Boolean
        get() = when (step) {
            BuilderStep.SITUATION -> situationText.isNotEmpty()
            BuilderStep.ACTION -> actionText.isNotEmpty()
            BuilderStep.PREVIEW -> true
            BuilderStep.REHEARSAL -> rehearsed
        }
}

@HiltViewModel
class PlanBuilderViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val riskWindowRepository: RiskWindowRepository,
    private val reminderScheduler: ReminderScheduler,
    triggerRepository: TriggerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanBuilderUiState())
    val uiState: StateFlow<PlanBuilderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val triggers = triggerRepository.activeTriggers.first()
                .filter { it.category != TriggerCategory.PLACE }
            _uiState.update { it.copy(situationTriggers = triggers) }
        }
    }

    fun selectTrigger(id: Long) = _uiState.update {
        it.copy(selectedTriggerId = if (it.selectedTriggerId == id) null else id, customSituation = "")
    }

    fun setCustomSituation(text: String) = _uiState.update {
        it.copy(customSituation = text, selectedTriggerId = null)
    }

    fun selectAction(action: String) = _uiState.update {
        it.copy(selectedAction = if (it.selectedAction == action) null else action, customAction = "")
    }

    fun setCustomAction(text: String) = _uiState.update {
        it.copy(customAction = text, selectedAction = null)
    }

    fun markRehearsed() = _uiState.update { it.copy(rehearsed = true) }

    fun next(onFinished: () -> Unit) {
        val state = _uiState.value
        if (!state.canContinue) return
        when (state.step) {
            BuilderStep.SITUATION -> _uiState.update { it.copy(step = BuilderStep.ACTION) }
            BuilderStep.ACTION -> _uiState.update { it.copy(step = BuilderStep.PREVIEW) }
            BuilderStep.PREVIEW -> _uiState.update { it.copy(step = BuilderStep.REHEARSAL) }
            BuilderStep.REHEARSAL -> save(onFinished)
        }
    }

    fun back(): Boolean {
        val state = _uiState.value
        val prev = when (state.step) {
            BuilderStep.SITUATION -> return false
            BuilderStep.ACTION -> BuilderStep.SITUATION
            BuilderStep.PREVIEW -> BuilderStep.ACTION
            BuilderStep.REHEARSAL -> BuilderStep.PREVIEW
        }
        _uiState.update { it.copy(step = prev) }
        return true
    }

    private fun save(onFinished: () -> Unit) {
        val state = _uiState.value
        if (state.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val planId = planRepository.createPlan(
                situationText = state.situationText,
                actionText = state.actionText,
                situationTriggerId = state.selectedTriggerId,
            )
            planRepository.recordRehearsal(planId)
            // First plan → derive risk windows from onboarding and start the
            // reminder chain at the next high-risk time.
            riskWindowRepository.ensureDefaultsFromOnboarding()
            reminderScheduler.scheduleNext(riskWindowRepository.enabledWindows())
            onFinished()
        }
    }
}
