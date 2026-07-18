package com.stillwater.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stillwater.app.data.RiskRepository
import com.stillwater.app.data.db.ResolvedEvent
import com.stillwater.app.data.db.UrgeDao
import com.stillwater.app.domain.RiskEngine
import com.stillwater.app.domain.TideSnapshot
import com.stillwater.app.domain.model.EntryPoint
import com.stillwater.app.domain.model.UrgeOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** One point per week for the sparkline, oldest first. */
data class WeekPoint(val label: String, val surfed: Int)

data class ProgressUiState(
    val totalSurfed: Int = 0,
    val surfedThisWeek: Int = 0,
    val metAtTheDoor: Int = 0,
    val weeklySurfed: List<WeekPoint> = emptyList(),
    /** Median hours from a slip back to the next surfed wave. Null = n/a. */
    val recoveryHours: Double? = null,
    val recoveryImproving: Boolean? = null,
    val hasAnyData: Boolean = false,
    val lapseCount: Int = 0,
    /** The tide engine's learned pattern, in calm sentences. Empty = still learning. */
    val patternLines: List<String> = emptyList(),
)

private const val WEEKS_SHOWN = 6

@HiltViewModel
class ProgressViewModel @Inject constructor(
    urgeDao: UrgeDao,
    riskRepository: RiskRepository,
) : ViewModel() {

    private val topTrigger = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProgressUiState> = kotlinx.coroutines.flow.combine(
        urgeDao.getResolvedEvents(),
        riskRepository.snapshot,
        topTrigger,
    ) { events, snapshot, trigger ->
        compute(events).copy(patternLines = patternLines(snapshot, trigger))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgressUiState(),
    )

    init {
        viewModelScope.launch { topTrigger.value = riskRepository.topTriggerName() }
    }

    private fun patternLines(snapshot: TideSnapshot, trigger: String?): List<String> {
        if (!snapshot.hasSignal) return emptyList()
        return buildList {
            RiskEngine.tideLine(snapshot)?.let(::add)
            snapshot.topMood?.let {
                add("Most of your waves ride in on feeling ${it.name.lowercase()}.")
            }
            trigger?.let { add("Your most common spark: ${it.lowercase()}.") }
            snapshot.typicalWaveMinutes?.let {
                add("Once you start surfing, waves typically pass in about $it minutes.")
            }
        }
    }

    private fun compute(events: List<ResolvedEvent>): ProgressUiState {
        val now = Instant.now()
        val surfed = events.filter { it.outcome == UrgeOutcome.SURFED.name }
        val lapses = events.filter { it.outcome == UrgeOutcome.LAPSED.name }
        val skippedAtDoor = events.count {
            it.entryPoint == EntryPoint.INTERCEPT.name &&
                it.outcome in listOf(UrgeOutcome.SKIPPED_APP.name, UrgeOutcome.SURFED.name)
        }

        val weekStart = now.truncatedTo(ChronoUnit.DAYS)
            .minus(((WEEKS_SHOWN - 1) * 7).toLong(), ChronoUnit.DAYS)
        val weekly = (0 until WEEKS_SHOWN).map { week ->
            val start = weekStart.plus((week * 7).toLong(), ChronoUnit.DAYS)
            val end = start.plus(7, ChronoUnit.DAYS)
            WeekPoint(
                label = if (week == WEEKS_SHOWN - 1) "now" else "",
                surfed = surfed.count {
                    val t = Instant.ofEpochMilli(it.startedAtEpochMs)
                    t >= start && t < end
                },
            )
        }

        // Recovery speed: for each lapse, hours until the next surfed wave.
        val recoveries = lapses.mapNotNull { lapse ->
            surfed.firstOrNull { it.startedAtEpochMs > lapse.startedAtEpochMs }
                ?.let { next ->
                    Duration.ofMillis(next.startedAtEpochMs - lapse.startedAtEpochMs)
                        .toMinutes() / 60.0
                }
        }
        val recent = recoveries.takeLast(3)
        val earlier = recoveries.dropLast(3)
        val improving = if (recent.isNotEmpty() && earlier.isNotEmpty()) {
            recent.median() < earlier.median()
        } else {
            null
        }

        val thisWeekStart = now.minus(7, ChronoUnit.DAYS)
        return ProgressUiState(
            totalSurfed = surfed.size,
            surfedThisWeek = surfed.count { Instant.ofEpochMilli(it.startedAtEpochMs) >= thisWeekStart },
            metAtTheDoor = skippedAtDoor,
            weeklySurfed = weekly,
            recoveryHours = recent.takeIf { it.isNotEmpty() }?.median(),
            recoveryImproving = improving,
            hasAnyData = events.isNotEmpty(),
            lapseCount = lapses.size,
        )
    }

    private fun List<Double>.median(): Double = sorted().let { s ->
        if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2
    }
}
