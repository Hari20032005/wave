package com.stillwater.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stillwater.app.data.UrgeRepository
import com.stillwater.app.data.prefs.UserPreferencesRepository
import com.stillwater.app.notification.QuickAccessNotification
import com.stillwater.app.service.MonitorService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Ready(val onboardingComplete: Boolean) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    preferencesRepository: UserPreferencesRepository,
    urgeRepository: UrgeRepository,
    quickAccessNotification: QuickAccessNotification,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = preferencesRepository.userPreferences
        .map { MainUiState.Ready(it.onboardingComplete) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainUiState.Loading,
        )

    init {
        viewModelScope.launch {
            // In-progress episodes that never resolved become honest data.
            urgeRepository.sweepAbandoned()
            // The quick-access notification doesn't survive reboots/swipes by
            // itself; re-post whenever the app runs. (Boot re-post lands with
            // M4's receiver.)
            val prefs = preferencesRepository.userPreferences.first()
            if (prefs.quickAccessEnabled) {
                quickAccessNotification.show()
            }
            // Protection survives app updates/force-stops by re-arming on open.
            if (prefs.interceptionEnabled) {
                MonitorService.start(appContext)
            }
        }
    }
}
