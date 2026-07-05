package com.stillwater.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per episode — surfed, lapsed, intercepted, or abandoned alike.
 * Inserted when a flow starts (outcome null = in progress); a sweep marks
 * stale in-progress rows ABANDONED so abandonment is honest data too.
 *
 * `mode` is null until known: BOTH-mode users say which pull it was in the
 * closing log. `shownPlanId` has no FK until if_then_plan ships in M3.
 */
@Entity(
    tableName = "urge_event",
    indices = [Index("startedAtEpochMs")],
)
data class UrgeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val localHourOfDay: Int,
    val localDayOfWeek: Int,
    val mode: String? = null,
    val entryPoint: String,
    val interceptedPackage: String? = null,
    val outcome: String? = null,
    val furthestStep: String? = null,
    val intensityBefore: Int? = null,
    val intensityAfter: Int? = null,
    val mood: String? = null,
    val shownPlanId: Long? = null,
    val note: String? = null,
)
