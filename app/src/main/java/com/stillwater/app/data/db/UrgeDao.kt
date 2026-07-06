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
