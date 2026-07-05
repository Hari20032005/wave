package com.stillwater.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "event_trigger",
    primaryKeys = ["urgeEventId", "triggerId"],
    foreignKeys = [
        ForeignKey(
            entity = UrgeEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["urgeEventId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TriggerEntity::class,
            parentColumns = ["id"],
            childColumns = ["triggerId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("triggerId")],
)
data class EventTriggerEntity(
    val urgeEventId: Long,
    val triggerId: Long,
)
