package com.stillwater.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lapse_debrief",
    foreignKeys = [
        ForeignKey(
            entity = UrgeEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["urgeEventId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("urgeEventId", unique = true)],
)
data class LapseDebriefEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val urgeEventId: Long,
    val completedAtEpochMs: Long,
    /** The situation before the lapse, in the user's own words. Optional. */
    val whatPreceded: String? = null,
    /** "What I'd try next time" — seeds if-then plans in M3. Optional. */
    val nextTimeIdea: String? = null,
    /** They can bail early; that's kind, and it's data. */
    val debriefCompleted: Boolean = false,
)
