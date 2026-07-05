package com.stillwater.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UserValueDao {

    @Query("SELECT * FROM user_value ORDER BY rank")
    fun getAll(): Flow<List<UserValueEntity>>

    @Query("DELETE FROM user_value")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(values: List<UserValueEntity>)

    /** Onboarding re-runs replace the whole set atomically. */
    @Transaction
    suspend fun replaceAll(values: List<UserValueEntity>) {
        deleteAll()
        insertAll(values)
    }
}
