package com.stillwater.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stillwater.app.data.RiskRepository
import com.stillwater.app.data.prefs.UserPreferencesRepository
import com.stillwater.app.domain.RiskEngine
import com.stillwater.app.notification.QuickAccessNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val quickAccessEnabled: Boolean = true, // default true = don't flash the setup card
    /** "High tide for you is usually around 11 pm." Null while still learning. */
    val tideLine: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val quickAccessNotification: QuickAccessNotification,
    riskRepository: RiskRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        preferencesRepository.userPreferences,
        riskRepository.snapshot,
    ) { prefs, snapshot ->
        HomeUiState(
            quickAccessEnabled = prefs.quickAccessEnabled,
            tideLine = RiskEngine.tideLine(snapshot),
        )
    }.stateIn(
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
