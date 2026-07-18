package com.stillwater.app.data

import com.stillwater.app.data.db.UrgeDao
import com.stillwater.app.domain.RiskEngine
import com.stillwater.app.domain.TideSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val WEEK_MS = 7 * 24 * 60 * 60 * 1000L

/** Wires the pure [RiskEngine] to the user's on-device history. */
@Singleton
class RiskRepository @Inject constructor(
    private val urgeDao: UrgeDao,
) {
    val snapshot: Flow<TideSnapshot> = urgeDao.getRiskEvents().map(RiskEngine::snapshot)

    suspend fun topTriggerName(): String? = urgeDao.topTriggerName()

    /** "Third time at this door this week — you walked away twice." */
    suspend fun doorMemory(packageName: String): String? {
        val since = System.currentTimeMillis() - WEEK_MS
        // +1: the visit currently being intercepted isn't logged yet.
        val visits = urgeDao.doorVisitsSince(packageName, since) + 1
        val walkedAway = urgeDao.doorWalkawaysSince(packageName, since)
        return RiskEngine.doorMemory(visits, walkedAway)
    }
}
