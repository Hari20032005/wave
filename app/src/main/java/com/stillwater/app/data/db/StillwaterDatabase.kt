package com.stillwater.app.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * v1 (M1): user_value. v2 (M2): urge_event, lapse_debrief, trigger,
 * event_trigger. v3 (M3): if_then_plan, risk_window. All added tables only,
 * so auto-migrations suffice. Exported schema JSON lives in app/schemas/
 * (checked in) so migrations get real tests. Trigger seeding happens
 * idempotently on open (see DataModule).
 */
@Database(
    entities = [
        UserValueEntity::class,
        UrgeEventEntity::class,
        LapseDebriefEntity::class,
        TriggerEntity::class,
        EventTriggerEntity::class,
        IfThenPlanEntity::class,
        RiskWindowEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class StillwaterDatabase : RoomDatabase() {
    abstract fun userValueDao(): UserValueDao
    abstract fun urgeDao(): UrgeDao
    abstract fun triggerDao(): TriggerDao
    abstract fun planDao(): PlanDao
    abstract fun riskWindowDao(): RiskWindowDao
}
