package com.stillwater.app.ui.protection

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stillwater.app.data.MonitoredAppRepository
import com.stillwater.app.data.RiskWindowRepository
import com.stillwater.app.data.prefs.UserPreferencesRepository
import com.stillwater.app.service.MonitorService
import com.stillwater.app.service.PermissionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProtectionUiState(
    val isPremium: Boolean = false,
    val interceptionEnabled: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val hasOverlay: Boolean = false,
    val hasBatteryExemption: Boolean = false,
    val monitoredCount: Int = 0,
    val windowLabels: List<String> = emptyList(),
) {
    val permissionsReady: Boolean get() = hasUsageAccess && hasOverlay
    val canEnable: Boolean get() = permissionsReady && monitoredCount > 0
}

@HiltViewModel
class ProtectionViewModel @Inject constructor(
    application: Application,
    private val preferencesRepository: UserPreferencesRepository,
    private val monitoredAppRepository: MonitoredAppRepository,
    private val riskWindowRepository: RiskWindowRepository,
    billingRepository: com.stillwater.app.data.billing.BillingRepository,
) : AndroidViewModel(application) {

    private val permissionState = MutableStateFlow(readPermissions())

    val uiState: StateFlow<ProtectionUiState> = combine(
        preferencesRepository.userPreferences,
        monitoredAppRepository.monitoredApps,
        riskWindowRepository.allWindows,
        permissionState,
        billingRepository.isPremium,
    ) { prefs, apps, windows, perms, premium ->
        ProtectionUiState(
            isPremium = premium,
            interceptionEnabled = prefs.interceptionEnabled,
            hasUsageAccess = perms.first,
            hasOverlay = perms.second,
            hasBatteryExemption = perms.third,
            monitoredCount = apps.count { it.isEnabled },
            windowLabels = windows.filter { it.isEnabled }.map { it.label },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProtectionUiState(),
    )

    /** Call from ON_RESUME — the user returns here from system settings. */
    fun refreshPermissions() {
        permissionState.value = readPermissions()
    }

    private fun readPermissions(): Triple<Boolean, Boolean, Boolean> {
        val context = getApplication<Application>()
        return Triple(
            PermissionStatus.hasUsageAccess(context),
            PermissionStatus.hasOverlay(context),
            PermissionStatus.hasBatteryExemption(context),
        )
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setInterceptionEnabled(enabled)
            val context = getApplication<Application>()
            if (enabled) MonitorService.start(context) else MonitorService.stop(context)
        }
    }

    // ---- Settings intents (each launched from an in-app disclosure card) ----

    fun usageAccessIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun overlayIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${getApplication<Application>().packageName}"),
    )

    @Suppress("BatteryLife")
    fun batteryExemptionIntent(): Intent = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${getApplication<Application>().packageName}"),
    )

    /**
     * OEM battery-killer settings, best effort. Every intent is optional —
     * the caller falls back to plain-text instructions when none resolve.
     */
    fun oemIntents(): List<Intent> {
        val byComponent = { pkg: String, cls: String ->
            Intent().setClassName(pkg, cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return when {
            Build.MANUFACTURER.contains("xiaomi", true) -> listOf(
                byComponent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            )
            Build.MANUFACTURER.contains("oppo", true) || Build.MANUFACTURER.contains("realme", true) -> listOf(
                byComponent("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                byComponent("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            )
            Build.MANUFACTURER.contains("vivo", true) -> listOf(
                byComponent("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            )
            Build.MANUFACTURER.contains("huawei", true) || Build.MANUFACTURER.contains("honor", true) -> listOf(
                byComponent("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            )
            Build.MANUFACTURER.contains("samsung", true) -> listOf(
                byComponent("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
            )
            else -> emptyList()
        }
    }

    fun oemName(): String = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
}
