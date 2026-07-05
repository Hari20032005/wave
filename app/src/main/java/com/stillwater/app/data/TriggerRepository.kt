package com.stillwater.app.data

import com.stillwater.app.data.db.TriggerDao
import com.stillwater.app.data.db.TriggerEntity
import com.stillwater.app.data.db.toModel
import com.stillwater.app.domain.model.Trigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriggerRepository @Inject constructor(
    dao: TriggerDao,
) {
    val activeTriggers: Flow<List<Trigger>> =
        dao.getActive().map { entities -> entities.map(TriggerEntity::toModel) }
}
