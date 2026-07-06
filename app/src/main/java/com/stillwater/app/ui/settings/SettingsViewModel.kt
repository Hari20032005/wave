package com.stillwater.app.ui.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stillwater.app.data.UserDataRepository
import com.stillwater.app.data.prefs.UserPreferencesRepository
import com.stillwater.app.notification.QuickAccessNotification
import com.stillwater.app.service.MonitorService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val protectionLockEnabled: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val preferencesRepository: UserPreferencesRepository,
    private val userDataRepository: UserDataRepository,
    private val quickAccessNotification: QuickAccessNotification,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = preferencesRepository.userPreferences
        .map { SettingsUiState(protectionLockEnabled = it.protectionLockEnabled) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setProtectionLock(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setProtectionLockEnabled(enabled) }
    }

    /** Build a share intent for the JSON export. Sharing is the user's choice. */
    fun export(onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val file = userDataRepository.exportToFile()
            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file,
            )
            onReady(
                Intent(Intent.ACTION_SEND)
                    .setType("application/json")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
        }
    }

    fun deleteEverything(onDone: () -> Unit) {
        viewModelScope.launch {
            MonitorService.stop(appContext)
            quickAccessNotification.hide()
            userDataRepository.deleteEverything()
            onDone()
        }
    }
}
