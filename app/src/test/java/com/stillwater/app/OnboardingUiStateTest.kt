package com.stillwater.app

import com.google.common.truth.Truth.assertThat
import com.stillwater.app.domain.model.Framing
import com.stillwater.app.domain.model.Mode
import com.stillwater.app.ui.onboarding.OnboardingStep
import com.stillwater.app.ui.onboarding.OnboardingUiState
import org.junit.Test

class OnboardingUiStateTest {

    @Test
    fun `high moral incongruence routes to values-first framing`() {
        val state = OnboardingUiState(mode = Mode.PORN, incongruenceAnswers = listOf(4, 4, 3))
        assertThat(state.framing).isEqualTo(Framing.VALUES_FIRST) // mean 3.67 >= 3.5
    }

    @Test
    fun `low moral incongruence keeps habit framing`() {
        val state = OnboardingUiState(mode = Mode.PORN, incongruenceAnswers = listOf(2, 3, 3))
        assertThat(state.framing).isEqualTo(Framing.HABIT_CHANGE)
    }

    @Test
    fun `unanswered incongruence defaults to habit framing`() {
        val state = OnboardingUiState(mode = Mode.PORN, incongruenceAnswers = listOf(5, null, 5))
        assertThat(state.framing).isEqualTo(Framing.HABIT_CHANGE)
    }

    @Test
    fun `incongruence step only shown for porn and both modes`() {
        assertThat(OnboardingUiState(mode = Mode.SOCIAL).steps)
            .doesNotContain(OnboardingStep.INCONGRUENCE)
        assertThat(OnboardingUiState(mode = Mode.PORN).steps)
            .contains(OnboardingStep.INCONGRUENCE)
        assertThat(OnboardingUiState(mode = Mode.BOTH).steps)
            .contains(OnboardingStep.INCONGRUENCE)
    }

    @Test
    fun `cannot continue past gates without answers`() {
        assertThat(OnboardingUiState(step = OnboardingStep.MODE).canContinue).isFalse()
        assertThat(OnboardingUiState(step = OnboardingStep.MODE, mode = Mode.SOCIAL).canContinue).isTrue()
        assertThat(OnboardingUiState(step = OnboardingStep.VALUES).canContinue).isFalse()
        assertThat(
            OnboardingUiState(
                step = OnboardingStep.VALUES,
                selectedValues = listOf("Self-respect"),
            ).canContinue,
        ).isTrue()
    }
}
