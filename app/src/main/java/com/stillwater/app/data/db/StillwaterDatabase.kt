package com.stillwater.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * v1 ships with M1 (`user_value` only). Later milestones add their tables via
 * migrations, per the approved SCHEMA_PROPOSAL.md. Exported schema JSON lives
 * in app/schemas/ (checked in) so migrations get real tests.
 */
@Database(
    entities = [UserValueEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class StillwaterDatabase : RoomDatabase() {
    abstract fun userValueDao(): UserValueDao
}
