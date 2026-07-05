package com.stillwater.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stillwater.app.data.prefs.UserPreferencesRepository
import com.stillwater.app.notification.QuickAccessNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val quickAccessEnabled: Boolean = true, // default true = don't flash the setup card
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val quickAccessNotification: QuickAccessNotification,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = preferencesRepository.userPreferences
        .map { HomeUiState(quickAccessEnabled = it.quickAccessEnabled) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    fun enableQuickAccess() {
        viewModelScope.launch {
            preferencesRepository.setQuickAccessEnabled(true)
            quickAccessNotification.show()
        }
    }
}
