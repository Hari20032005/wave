package com.stillwater.app.ui.protection

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stillwater.app.data.MonitoredAppRepository
import com.stillwater.app.ui.components.CalmQuietButton
import com.stillwater.app.ui.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PickableApp(
    val packageName: String,
    val label: String,
    val monitored: Boolean,
)

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    application: Application,
    private val monitoredAppRepository: MonitoredAppRepository,
) : AndroidViewModel(application) {

    private val installed = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    val apps: StateFlow<List<PickableApp>> = combine(
        installed,
        monitoredAppRepository.monitoredApps,
    ) { installedApps, monitored ->
        val monitoredSet = monitored.filter { it.isEnabled }.map { it.packageName }.toSet()
        installedApps.map { (pkg, label) ->
            PickableApp(pkg, label, pkg in monitoredSet)
        }.sortedWith(compareByDescending<PickableApp> { it.monitored }.thenBy { it.label.lowercase() })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            installed.value = withContext(Dispatchers.IO) { loadLaunchableApps() }
        }
    }

    private fun loadLaunchableApps(): List<Pair<String, String>> {
        val pm = getApplication<Application>().packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .filter { it.first != getApplication<Application>().packageName }
    }

    fun toggle(app: PickableApp) {
        viewModelScope.launch {
            monitoredAppRepository.setMonitored(app.packageName, app.label, !app.monitored)
        }
    }
}

@Composable
fun AppPickerScreen(
    onBack: () -> Unit,
    viewModel: AppPickerViewModel = hiltViewModel(),
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = Spacing.screenEdge, vertical = Spacing.lg),
    ) {
        Text(
            text = "Which apps pull at you?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "Stillwater will meet you at the door of the apps you check.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            items(apps, key = { it.packageName }) { app ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    // Whole row toggles — a checkbox-only target is too small.
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = app.monitored,
                            role = Role.Checkbox,
                            onValueChange = { viewModel.toggle(app) },
                        )
                        .padding(vertical = Spacing.xs),
                ) {
                    Checkbox(
                        checked = app.monitored,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        CalmQuietButton(
            text = "Done",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
