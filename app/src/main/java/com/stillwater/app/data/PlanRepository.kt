package com.stillwater.app.data

import com.stillwater.app.data.db.IfThenPlanEntity
import com.stillwater.app.data.db.PlanDao
import com.stillwater.app.data.db.toModel
import com.stillwater.app.domain.model.IfThenPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Free tier: one active plan. Lifted with Premium in M5. */
const val MAX_ACTIVE_PLANS_FREE = 1

@Singleton
class PlanRepository @Inject constructor(
    private val dao: PlanDao,
) {
    val activePlans: Flow<List<IfThenPlan>> =
        dao.getActivePlans().map { entities -> entities.map(IfThenPlanEntity::toModel) }

    suspend fun currentPlan(): IfThenPlan? = dao.getCurrentPlan()?.toModel()

    suspend fun canCreatePlan(): Boolean = dao.countActive() < MAX_ACTIVE_PLANS_FREE

    suspend fun createPlan(
        situationText: String,
        actionText: String,
        situationTriggerId: Long?,
    ): Long {
        val now = System.currentTimeMillis()
        return dao.insert(
            IfThenPlanEntity(
                situationText = situationText,
                actionText = actionText,
                situationTriggerId = situationTriggerId,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
    }

    suspend fun updatePlan(
        id: Long,
        situationText: String,
        actionText: String,
        situationTriggerId: Long?,
    ) {
        dao.updateContent(id, situationText, actionText, situationTriggerId, System.currentTimeMillis())
    }

    suspend fun recordRehearsal(id: Long) = dao.recordRehearsal(id, System.currentTimeMillis())

    suspend fun recordShown(id: Long) = dao.recordShown(id)
}
