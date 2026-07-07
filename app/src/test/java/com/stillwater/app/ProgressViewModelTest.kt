package com.stillwater.app

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stillwater.app.data.db.EventTriggerEntity
import com.stillwater.app.data.db.LapseDebriefEntity
import com.stillwater.app.data.db.ResolvedEvent
import com.stillwater.app.data.db.UrgeDao
import com.stillwater.app.data.db.UrgeEventEntity
import com.stillwater.app.ui.progress.ProgressViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** Hand-rolled fake (interface, so no mocking library needed — per the skill's doubles guidance). */
private class FakeUrgeDao(
    events: List<ResolvedEvent> = emptyList(),
) : UrgeDao {
    val resolved = MutableStateFlow(events)

    override fun getResolvedEvents(): Flow<List<ResolvedEvent>> = resolved
    override suspend fun insertEvent(event: UrgeEventEntity): Long = 0
    override suspend fun completeEvent(
        id: Long, endedAtEpochMs: Long, outcome: String, furthestStep: String?,
        mode: String?, mood: String?, intensityAfter: Int?, shownPlanId: Long?, note: String?,
    ) = Unit

    override suspend fun insertEventTriggers(links: List<EventTriggerEntity>) = Unit
    override suspend fun insertDebrief(debrief: LapseDebriefEntity) = Unit
    override suspend fun dumpEvents(): List<UrgeEventEntity> = emptyList()
    override suspend fun dumpDebriefs(): List<LapseDebriefEntity> = emptyList()
    override suspend fun dumpEventTriggers(): List<EventTriggerEntity> = emptyList()
    override suspend fun markStaleAsAbandoned(cutoffEpochMs: Long, nowEpochMs: Long) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun event(daysAgo: Long, outcome: String, entryPoint: String = "IN_APP") =
        ResolvedEvent(
            startedAtEpochMs = Instant.now().minus(Duration.ofDays(daysAgo)).toEpochMilli(),
            outcome = outcome,
            entryPoint = entryPoint,
        )

    @Test
    fun `counts surfed waves and intercept wins`() = runTest {
        val dao = FakeUrgeDao(
            listOf(
                event(20, "SURFED"),
                event(2, "SURFED"),
                event(1, "SKIPPED_APP", entryPoint = "INTERCEPT"),
                event(1, "CONTINUED", entryPoint = "INTERCEPT"),
                event(0, "ABANDONED"),
            ),
        )
        ProgressViewModel(dao).uiState.test {
            awaitItem() // initial empty
            val state = awaitItem()
            assertThat(state.totalSurfed).isEqualTo(2)
            assertThat(state.surfedThisWeek).isEqualTo(1)
            assertThat(state.metAtTheDoor).isEqualTo(1) // skip counts, continue doesn't
            assertThat(state.weeklySurfed).hasSize(6)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recovery speed is hours from a lapse to the next surfed wave`() = runTest {
        val lapseAt = Instant.now().minus(Duration.ofDays(3))
        val backAt = lapseAt.plus(Duration.ofHours(6))
        val dao = FakeUrgeDao(
            listOf(
                ResolvedEvent(lapseAt.toEpochMilli(), "LAPSED", "IN_APP"),
                ResolvedEvent(backAt.toEpochMilli(), "SURFED", "IN_APP"),
            ),
        )
        ProgressViewModel(dao).uiState.test {
            awaitItem()
            val state = awaitItem()
            assertThat(state.lapseCount).isEqualTo(1)
            assertThat(state.recoveryHours).isWithin(0.1).of(6.0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no data yields the empty state, never fake numbers`() = runTest {
        ProgressViewModel(FakeUrgeDao()).uiState.test {
            awaitItem()
            val state = awaitItem()
            assertThat(state.hasAnyData).isFalse()
            assertThat(state.recoveryHours).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
