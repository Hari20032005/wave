package com.stillwater.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stillwater.app.domain.model.IfThenPlan
import com.stillwater.app.domain.model.Mode

/**
 * An implementation intention, always in strict "If [situation], then I will
 * [action]" form. `situationTriggerId` links the situation to the trigger
 * taxonomy so the SOS flow can surface the matching plan (no FK constraint —
 * triggers with plans attached must survive taxonomy edits).
 */
@Entity(tableName = "if_then_plan")
data class IfThenPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val situationText: String,
    val actionText: String,
    val situationTriggerId: Long? = null,
    val mode: String? = null,
    val isActive: Boolean = true,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val rehearsalCount: Int = 0,
    val lastRehearsedAtEpochMs: Long? = null,
    val timesShown: Int = 0,
    val timesMarkedUsed: Int = 0,
)

fun IfThenPlanEntity.toModel() = IfThenPlan(
    id = id,
    situationText = situationText,
    actionText = actionText,
    situationTriggerId = situationTriggerId,
    mode = mode?.let { runCatching { Mode.valueOf(it) }.getOrNull() },
    isActive = isActive,
    rehearsalCount = rehearsalCount,
)
