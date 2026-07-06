package com.stillwater.app.data

import com.stillwater.app.data.db.EventTriggerEntity
import com.stillwater.app.data.db.LapseDebriefEntity
import com.stillwater.app.data.db.UrgeDao
import com.stillwater.app.data.db.UrgeEventEntity
import com.stillwater.app.domain.model.EntryPoint
import com.stillwater.app.domain.model.Mode
import com.stillwater.app.domain.model.MoodTag
import com.stillwater.app.domain.model.SosStep
import com.stillwater.app.domain.model.UrgeOutcome
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** In-progress rows older than this are swept to ABANDONED at app start. */
private const val STALE_EVENT_MS = 15 * 60 * 1000L

@Singleton
class UrgeRepository @Inject constructor(
    private val urgeDao: UrgeDao,
) {

    /** Insert at flow start; outcome stays null until resolution. */
    suspend fun startEvent(
        entryPoint: EntryPoint,
        mode: Mode?,
        interceptedPackage: String? = null,
    ): Long {
        val now = ZonedDateTime.now()
        return urgeDao.insertEvent(
            UrgeEventEntity(
                startedAtEpochMs = now.toInstant().toEpochMilli(),
                localHourOfDay = now.hour,
                localDayOfWeek = now.dayOfWeek.value,
                // BOTH isn't an event-level mode; those users choose in the log.
                mode = mode?.takeIf { it != Mode.BOTH }?.name,
                entryPoint = entryPoint.name,
                interceptedPackage = interceptedPackage,
            ),
        )
    }

    /**
     * One-shot log for intercept decisions taken on the overlay itself
     * ("continue anyway" / "I'll skip it") — both outcomes are equally
     * valuable to the trigger model.
     */
    suspend fun logInterceptOutcome(
        interceptedPackage: String,
        mode: Mode?,
        outcome: UrgeOutcome,
    ) {
        val id = startEvent(EntryPoint.INTERCEPT, mode, interceptedPackage)
        urgeDao.completeEvent(
            id = id,
            endedAtEpochMs = System.currentTimeMillis(),
            outcome = outcome.name,
            furthestStep = null,
            mode = mode?.takeIf { it != Mode.BOTH }?.name,
            mood = null,
            intensityAfter = null,
            shownPlanId = null,
            note = null,
        )
    }

    suspend fun completeEvent(
        eventId: Long,
        outcome: UrgeOutcome,
        furthestStep: SosStep?,
        eventMode: Mode?,
        mood: MoodTag?,
        intensityAfter: Int? = null,
        triggerIds: List<Long> = emptyList(),
        shownPlanId: Long? = null,
        note: String? = null,
    ) {
        urgeDao.completeEvent(
            id = eventId,
            endedAtEpochMs = System.currentTimeMillis(),
            outcome = outcome.name,
            furthestStep = furthestStep?.name,
            mode = eventMode?.takeIf { it != Mode.BOTH }?.name,
            mood = mood?.name,
            intensityAfter = intensityAfter,
            shownPlanId = shownPlanId,
            note = note,
        )
        if (triggerIds.isNotEmpty()) {
            urgeDao.insertEventTriggers(
                triggerIds.map { EventTriggerEntity(urgeEventId = eventId, triggerId = it) },
            )
        }
    }

    suspend fun attachDebrief(
        eventId: Long,
        whatPreceded: String?,
        nextTimeIdea: String?,
        completed: Boolean,
    ) {
        urgeDao.insertDebrief(
            LapseDebriefEntity(
                urgeEventId = eventId,
                completedAtEpochMs = System.currentTimeMillis(),
                whatPreceded = whatPreceded?.takeIf { it.isNotBlank() },
                nextTimeIdea = nextTimeIdea?.takeIf { it.isNotBlank() },
                debriefCompleted = completed,
            ),
        )
    }

    suspend fun sweepAbandoned() {
        val now = System.currentTimeMillis()
        urgeDao.markStaleAsAbandoned(cutoffEpochMs = now - STALE_EVENT_MS, nowEpochMs = now)
    }
}
