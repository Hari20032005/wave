package com.stillwater.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredAppDao {

    @Query("SELECT * FROM monitored_app ORDER BY label")
    fun getAll(): Flow<List<MonitoredAppEntity>>

    @Query("SELECT packageName FROM monitored_app WHERE isEnabled = 1")
    suspend fun getEnabledPackages(): List<String>

    @Upsert
    suspend fun upsert(app: MonitoredAppEntity)

    @Query("DELETE FROM monitored_app WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
