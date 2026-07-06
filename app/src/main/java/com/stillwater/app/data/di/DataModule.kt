package com.stillwater.app.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stillwater.app.data.db.PlanDao
import com.stillwater.app.data.db.RiskWindowDao
import com.stillwater.app.data.db.SEED_TRIGGERS
import com.stillwater.app.data.db.StillwaterDatabase
import com.stillwater.app.data.db.TriggerDao
import com.stillwater.app.data.db.UrgeDao
import com.stillwater.app.data.db.UserValueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StillwaterDatabase =
        Room.databaseBuilder(context, StillwaterDatabase::class.java, "stillwater.db")
            .addCallback(SeedTriggersCallback)
            .build()

    /**
     * Idempotent seed on every open (fixed ids + INSERT OR IGNORE) — one code
     * path covers fresh installs and migrated installs alike.
     */
    private object SeedTriggersCallback : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            SEED_TRIGGERS.forEach { trigger ->
                db.execSQL(
                    "INSERT OR IGNORE INTO trigger (id, name, category, isCustom, isArchived) " +
                        "VALUES (?, ?, ?, 0, 0)",
                    arrayOf<Any>(trigger.id, trigger.name, trigger.category),
                )
            }
        }
    }

    @Provides
    fun provideUserValueDao(db: StillwaterDatabase): UserValueDao = db.userValueDao()

    @Provides
    fun provideUrgeDao(db: StillwaterDatabase): UrgeDao = db.urgeDao()

    @Provides
    fun provideTriggerDao(db: StillwaterDatabase): TriggerDao = db.triggerDao()

    @Provides
    fun providePlanDao(db: StillwaterDatabase): PlanDao = db.planDao()

    @Provides
    fun provideRiskWindowDao(db: StillwaterDatabase): RiskWindowDao = db.riskWindowDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("user_prefs") },
    )
}
