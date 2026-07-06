package com.stillwater.app.data

import com.stillwater.app.data.db.RiskWindowDao
import com.stillwater.app.data.db.RiskWindowEntity
import com.stillwater.app.data.prefs.UserPreferencesRepository
import com.stillwater.app.domain.model.TriggerTime
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private const val WEEKDAYS = 0b0011111 // Mon–Fri
private const val ALL_DAYS = 0b1111111
private const val WEEKEND = 0b1100000 // Sat–Sun

@Singleton
class RiskWindowRepository @Inject constructor(
    private val dao: RiskWindowDao,
    private val preferencesRepository: UserPreferencesRepository,
) {
    suspend fun enabledWindows(): List<RiskWindowEntity> = dao.getEnabled()

    val allWindows: kotlinx.coroutines.flow.Flow<List<RiskWindowEntity>> = dao.getAll()

    /**
     * Derive default windows from the onboarding trigger-time answers, once.
     * (WHEN_ALONE / UNPREDICTABLE have no clock shape — nothing to derive.)
     */
    suspend fun ensureDefaultsFromOnboarding() {
        if (dao.count() > 0) return
        val times = preferencesRepository.userPreferences.first().triggerTimes
        val defaults = buildList {
            if (TriggerTime.LATE_NIGHT in times) {
                add(RiskWindowEntity(label = "Late night", daysOfWeekMask = ALL_DAYS, startMinuteOfDay = 22 * 60, endMinuteOfDay = 2 * 60))
            }
            if (TriggerTime.MORNING in times) {
                add(RiskWindowEntity(label = "Mornings", daysOfWeekMask = ALL_DAYS, startMinuteOfDay = 6 * 60 + 30, endMinuteOfDay = 9 * 60))
            }
            if (TriggerTime.AFTER_WORK in times) {
                add(RiskWindowEntity(label = "After work", daysOfWeekMask = WEEKDAYS, startMinuteOfDay = 17 * 60 + 30, endMinuteOfDay = 20 * 60))
            }
            if (TriggerTime.WEEKEND in times) {
                add(RiskWindowEntity(label = "Weekends", daysOfWeekMask = WEEKEND, startMinuteOfDay = 19 * 60, endMinuteOfDay = 23 * 60))
            }
        }
        if (defaults.isNotEmpty()) dao.insertAll(defaults)
    }
}
