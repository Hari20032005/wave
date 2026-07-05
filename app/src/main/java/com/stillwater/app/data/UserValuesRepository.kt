package com.stillwater.app.data

import com.stillwater.app.data.db.UserValueDao
import com.stillwater.app.data.db.UserValueEntity
import com.stillwater.app.data.db.toModel
import com.stillwater.app.domain.model.UserValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserValuesRepository @Inject constructor(
    private val dao: UserValueDao,
) {
    val values: Flow<List<UserValue>> =
        dao.getAll().map { entities -> entities.map(UserValueEntity::toModel) }

    suspend fun replaceAll(names: List<String>) {
        dao.replaceAll(names.mapIndexed { index, name -> UserValueEntity(name = name, rank = index) })
    }
}
