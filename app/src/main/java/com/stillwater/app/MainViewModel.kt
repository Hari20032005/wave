package com.stillwater.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stillwater.app.data.prefs.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Ready(val onboardingComplete: Boolean) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = preferencesRepository.userPreferences
        .map { MainUiState.Ready(it.onboardingComplete) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainUiState.Loading,
        )
}
