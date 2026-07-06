package com.stillwater.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A recurring high-risk time span. Created from the onboarding trigger-time
 * answers; drives plan reminders (M3) and interception windows (M4).
 * `daysOfWeekMask`: Mon = 1<<0 … Sun = 1<<6. Windows may wrap past midnight
 * (startMinuteOfDay > endMinuteOfDay).
 */
@Entity(tableName = "risk_window")
data class RiskWindowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val daysOfWeekMask: Int,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val isEnabled: Boolean = true,
)
