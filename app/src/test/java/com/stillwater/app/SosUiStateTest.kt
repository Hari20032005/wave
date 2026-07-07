package com.stillwater.app

import com.google.common.truth.Truth.assertThat
import com.stillwater.app.domain.model.Mode
import com.stillwater.app.domain.model.MoodTag
import com.stillwater.app.domain.model.Trigger
import com.stillwater.app.domain.model.TriggerCategory
import com.stillwater.app.ui.sos.SURF_SCRIPT
import com.stillwater.app.ui.sos.SosUiState
import org.junit.Test

class SosUiStateTest {

    private val triggers = listOf(
        Trigger(1, "Scrolling a feed", TriggerCategory.ACTIVITY),
        Trigger(5, "Couldn't sleep", TriggerCategory.TIME),
        Trigger(7, "In bed", TriggerCategory.PLACE),
    )

    @Test
    fun `surf script advances with elapsed time and never regresses`() {
        assertThat(SosUiState(surfElapsedSeconds = 0).surfLine).isEqualTo(SURF_SCRIPT[0].second)
        assertThat(SosUiState(surfElapsedSeconds = 20).surfLine).isEqualTo(SURF_SCRIPT[1].second)
        assertThat(SosUiState(surfElapsedSeconds = 89).surfLine).isEqualTo(SURF_SCRIPT.last().second)
    }

    @Test
    fun `places and sparks are split by category`() {
        val state = SosUiState(triggers = triggers)
        assertThat(state.placeTriggers.map { it.id }).containsExactly(7L)
        assertThat(state.sparkTriggers.map { it.id }).containsExactly(1L, 5L)
    }

    @Test
    fun `both-mode users must also pick which pull it was`() {
        val base = SosUiState(
            profileMode = Mode.BOTH,
            mood = MoodTag.BORED,
            sparkTriggerId = 1,
            placeTriggerId = 7,
        )
        assertThat(base.canSaveLog).isFalse()
        assertThat(base.copy(chosenMode = Mode.PORN).canSaveLog).isTrue()
    }

    @Test
    fun `single-mode users save with three taps`() {
        val state = SosUiState(
            profileMode = Mode.SOCIAL,
            mood = MoodTag.STRESSED,
            sparkTriggerId = 1,
            placeTriggerId = 7,
        )
        assertThat(state.canSaveLog).isTrue()
    }
}
