package com.stillwater.app

import com.google.common.truth.Truth.assertThat
import com.stillwater.app.domain.model.Trigger
import com.stillwater.app.domain.model.TriggerCategory
import com.stillwater.app.ui.plans.BuilderStep
import com.stillwater.app.ui.plans.PlanBuilderUiState
import org.junit.Test

class PlanBuilderUiStateTest {

    private val triggers = listOf(
        Trigger(5, "Couldn't sleep", TriggerCategory.TIME),
        Trigger(99, "Custom trigger", TriggerCategory.ACTIVITY, isCustom = true),
    )

    @Test
    fun `seed trigger labels become first-person situation clauses`() {
        // the phrase map fixed the "If couldn't sleep" grammar bug
        val state = PlanBuilderUiState(
            situationTriggers = triggers,
            selectedTriggerId = 5,
            selectedAction = "take ten slow breaths",
        )
        assertThat(state.situationText).isEqualTo("I can't sleep")
        assertThat(state.sentence)
            .isEqualTo("If I can't sleep, then I will take ten slow breaths.")
    }

    @Test
    fun `unmapped triggers fall back to lowercased label`() {
        val state = PlanBuilderUiState(situationTriggers = triggers, selectedTriggerId = 99)
        assertThat(state.situationText).isEqualTo("custom trigger")
    }

    @Test
    fun `custom text wins over chip selection`() {
        val state = PlanBuilderUiState(
            situationTriggers = triggers,
            selectedTriggerId = 5,
            customSituation = "I'm home alone on a Friday",
        )
        assertThat(state.situationText).isEqualTo("I'm home alone on a Friday")
    }

    @Test
    fun `rehearsal gates the final save`() {
        val ready = PlanBuilderUiState(
            step = BuilderStep.REHEARSAL,
            situationTriggers = triggers,
            selectedTriggerId = 5,
            selectedAction = "take ten slow breaths",
        )
        assertThat(ready.canContinue).isFalse()
        assertThat(ready.copy(rehearsed = true).canContinue).isTrue()
    }
}
