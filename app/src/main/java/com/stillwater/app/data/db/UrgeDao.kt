package com.stillwater.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Lightweight projection for progress computations. */
data class ResolvedEvent(
    val startedAtEpochMs: Long,
    val outcome: String,
    val entryPoint: String,
)

@Dao
interface UrgeDao {

    @Insert
    suspend fun insertEvent(event: UrgeEventEntity): Long

    @Query(
        """
        UPDATE urge_event SET
            endedAtEpochMs = :endedAtEpochMs,
            outcome = :outcome,
            furthestStep = :furthestStep,
            mode = :mode,
            mood = :mood,
            intensityAfter = :intensityAfter,
            shownPlanId = :shownPlanId,
            note = :note
        WHERE id = :id
        """,
    )
    suspend fun completeEvent(
        id: Long,
        endedAtEpochMs: Long,
        outcome: String,
        furthestStep: String?,
        mode: String?,
        mood: String?,
        intensityAfter: Int?,
        shownPlanId: Long?,
        note: String?,
    )

    @Insert
    suspend fun insertEventTriggers(links: List<EventTriggerEntity>)

    @Insert
    suspend fun insertDebrief(debrief: LapseDebriefEntity)

    @Query(
        """
        SELECT startedAtEpochMs, outcome, entryPoint FROM urge_event
        WHERE outcome IS NOT NULL ORDER BY startedAtEpochMs
        """,
    )
    fun getResolvedEvents(): kotlinx.coroutines.flow.Flow<List<ResolvedEvent>>

    // ---- Pattern engine feeds (Tide) ----
    @Query(
        """
        SELECT startedAtEpochMs, endedAtEpochMs, localHourOfDay, localDayOfWeek,
               mood, outcome, entryPoint, interceptedPackage
        FROM urge_event WHERE outcome IS NOT NULL ORDER BY startedAtEpochMs
        """,
    )
    fun getRiskEvents(): kotlinx.coroutines.flow.Flow<List<com.stillwater.app.domain.RiskEvent>>

    /** Most frequently logged trigger, for the "your pattern" card. */
    @Query(
        """
        SELECT t.name FROM event_trigger et
        JOIN trigger t ON t.id = et.triggerId
        GROUP BY et.triggerId ORDER BY COUNT(*) DESC LIMIT 1
        """,
    )
    suspend fun topTriggerName(): String?

    /** Door stats for one app over a recent period: total visits + walked away. */
    @Query(
        """
        SELECT COUNT(*) FROM urge_event
        WHERE interceptedPackage = :packageName AND startedAtEpochMs >= :sinceEpochMs
        """,
    )
    suspend fun doorVisitsSince(packageName: String, sinceEpochMs: Long): Int

    @Query(
        """
        SELECT COUNT(*) FROM urge_event
        WHERE interceptedPackage = :packageName AND startedAtEpochMs >= :sinceEpochMs
          AND outcome IN ('SKIPPED_APP', 'SURFED')
        """,
    )
    suspend fun doorWalkawaysSince(packageName: String, sinceEpochMs: Long): Int

    // ---- Full dumps for the user's data export (M6) ----
    @Query("SELECT * FROM urge_event ORDER BY id")
    suspend fun dumpEvents(): List<UrgeEventEntity>

    @Query("SELECT * FROM lapse_debrief ORDER BY id")
    suspend fun dumpDebriefs(): List<LapseDebriefEntity>

    @Query("SELECT * FROM event_trigger")
    suspend fun dumpEventTriggers(): List<EventTriggerEntity>

    /** Flows that never resolved (process death, swiped away) become honest data. */
    @Query(
        """
        UPDATE urge_event SET outcome = 'ABANDONED', endedAtEpochMs = :nowEpochMs
        WHERE outcome IS NULL AND startedAtEpochMs < :cutoffEpochMs
        """,
    )
    suspend fun markStaleAsAbandoned(cutoffEpochMs: Long, nowEpochMs: Long)
}
