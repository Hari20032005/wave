package com.stillwater.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stillwater.app.domain.model.UserValue

@Entity(tableName = "user_value")
data class UserValueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "rank") val rank: Int,
)

fun UserValueEntity.toModel() = UserValue(id = id, name = name, rank = rank)
fun UserValue.toEntity() = UserValueEntity(id = id, name = name, rank = rank)
