package com.stillwater.app.data.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TriggerDao {

    @Query("SELECT * FROM trigger WHERE isArchived = 0 ORDER BY id")
    fun getActive(): Flow<List<TriggerEntity>>
}
