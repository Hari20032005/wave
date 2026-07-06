package com.stillwater.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Query("SELECT * FROM if_then_plan WHERE isActive = 1 ORDER BY updatedAtEpochMs DESC")
    fun getActivePlans(): Flow<List<IfThenPlanEntity>>

    @Query("SELECT * FROM if_then_plan WHERE isActive = 1 ORDER BY updatedAtEpochMs DESC LIMIT 1")
    suspend fun getCurrentPlan(): IfThenPlanEntity?

    @Insert
    suspend fun insert(plan: IfThenPlanEntity): Long

    @Query(
        """
        UPDATE if_then_plan SET
            situationText = :situationText,
            actionText = :actionText,
            situationTriggerId = :situationTriggerId,
            updatedAtEpochMs = :updatedAtEpochMs
        WHERE id = :id
        """,
    )
    suspend fun updateContent(
        id: Long,
        situationText: String,
        actionText: String,
        situationTriggerId: Long?,
        updatedAtEpochMs: Long,
    )

    @Query(
        """
        UPDATE if_then_plan SET
            rehearsalCount = rehearsalCount + 1,
            lastRehearsedAtEpochMs = :nowEpochMs
        WHERE id = :id
        """,
    )
    suspend fun recordRehearsal(id: Long, nowEpochMs: Long)

    @Query("UPDATE if_then_plan SET timesShown = timesShown + 1 WHERE id = :id")
    suspend fun recordShown(id: Long)

    @Query("SELECT COUNT(*) FROM if_then_plan WHERE isActive = 1")
    suspend fun countActive(): Int
}
