package com.stillwater.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RiskWindowDao {

    @Query("SELECT * FROM risk_window ORDER BY id")
    fun getAll(): Flow<List<RiskWindowEntity>>

    @Query("SELECT * FROM risk_window WHERE isEnabled = 1 ORDER BY id")
    suspend fun getEnabled(): List<RiskWindowEntity>

    @Query("SELECT COUNT(*) FROM risk_window")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(windows: List<RiskWindowEntity>)
}
