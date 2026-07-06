package com.stillwater.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.stillwater.app.domain.model.Framing
import com.stillwater.app.domain.model.Mode
import com.stillwater.app.domain.model.MoodTag
import com.stillwater.app.domain.model.TriggerTime
import com.stillwater.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val TRIGGER_TIMES = stringSetPreferencesKey("trigger_times")
        val COMMON_MOODS = stringSetPreferencesKey("common_moods")
        val MORAL_INCONGRUENCE = floatPreferencesKey("moral_incongruence")
        val FRAMING = stringPreferencesKey("framing")
        val QUICK_ACCESS_ENABLED = booleanPreferencesKey("quick_access_enabled")
        val INTERCEPTION_ENABLED = booleanPreferencesKey("interception_enabled")
        val PROTECTION_LOCK_ENABLED = booleanPreferencesKey("protection_lock_enabled")
    }

    val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            mode = prefs[Keys.MODE]?.let { runCatching { Mode.valueOf(it) }.getOrNull() },
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false,
            triggerTimes = prefs[Keys.TRIGGER_TIMES].toEnums<TriggerTime>(),
            commonMoods = prefs[Keys.COMMON_MOODS].toEnums<MoodTag>(),
            moralIncongruenceScore = prefs[Keys.MORAL_INCONGRUENCE],
            framing = prefs[Keys.FRAMING]
                ?.let { runCatching { Framing.valueOf(it) }.getOrNull() }
                ?: Framing.HABIT_CHANGE,
            quickAccessEnabled = prefs[Keys.QUICK_ACCESS_ENABLED] ?: false,
            interceptionEnabled = prefs[Keys.INTERCEPTION_ENABLED] ?: false,
            protectionLockEnabled = prefs[Keys.PROTECTION_LOCK_ENABLED] ?: false,
        )
    }

    suspend fun setProtectionLockEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.PROTECTION_LOCK_ENABLED] = enabled }
    }

    suspend fun setQuickAccessEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.QUICK_ACCESS_ENABLED] = enabled }
    }

    suspend fun setInterceptionEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.INTERCEPTION_ENABLED] = enabled }
    }

    /** Single atomic write at the end of onboarding. */
    suspend fun completeOnboarding(
        mode: Mode,
        triggerTimes: Set<TriggerTime>,
        commonMoods: Set<MoodTag>,
        moralIncongruenceScore: Float?,
        framing: Framing,
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.MODE] = mode.name
            prefs[Keys.TRIGGER_TIMES] = triggerTimes.mapTo(mutableSetOf()) { it.name }
            prefs[Keys.COMMON_MOODS] = commonMoods.mapTo(mutableSetOf()) { it.name }
            moralIncongruenceScore?.let { prefs[Keys.MORAL_INCONGRUENCE] = it }
            prefs[Keys.FRAMING] = framing.name
            prefs[Keys.ONBOARDING_COMPLETE] = true
        }
    }

    private inline fun <reified E : Enum<E>> Set<String>?.toEnums(): Set<E> =
        this?.mapNotNullTo(mutableSetOf()) { name ->
            runCatching { enumValueOf<E>(name) }.getOrNull()
        } ?: emptySet()
}
