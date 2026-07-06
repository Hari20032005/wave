package com.stillwater.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An app the user asked Stillwater to stand between them and. Only the
 * package name and cached label — never usage content (Principle 1/5).
 */
@Entity(tableName = "monitored_app")
data class MonitoredAppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val isEnabled: Boolean = true,
)
