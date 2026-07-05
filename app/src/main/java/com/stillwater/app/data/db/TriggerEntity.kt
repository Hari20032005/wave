package com.stillwater.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stillwater.app.domain.model.Trigger
import com.stillwater.app.domain.model.TriggerCategory

@Entity(tableName = "trigger")
data class TriggerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val isCustom: Boolean = false,
    val isArchived: Boolean = false,
)

fun TriggerEntity.toModel() = Trigger(
    id = id,
    name = name,
    category = runCatching { TriggerCategory.valueOf(category) }
        .getOrDefault(TriggerCategory.ACTIVITY),
    isCustom = isCustom,
)

/**
 * Curated seed set with fixed ids, inserted with INSERT OR IGNORE on every DB
 * open (idempotent — covers fresh installs and migrated installs alike).
 * "Sparks" are what set the urge off; "places" give context. Feelings are the
 * `mood` column, not triggers, so the closing log stays three taps.
 */
val SEED_TRIGGERS = listOf(
    TriggerEntity(1, "Scrolling a feed", "ACTIVITY"),
    TriggerEntity(2, "Just opened my phone", "ACTIVITY"),
    TriggerEntity(3, "Saw something triggering", "ACTIVITY"),
    TriggerEntity(4, "Putting something off", "ACTIVITY"),
    TriggerEntity(5, "Couldn't sleep", "TIME"),
    TriggerEntity(6, "After an argument", "SOCIAL"),
    TriggerEntity(7, "In bed", "PLACE"),
    TriggerEntity(8, "Alone in my room", "PLACE"),
    TriggerEntity(9, "At my desk", "PLACE"),
    TriggerEntity(10, "Somewhere else", "PLACE"),
)
