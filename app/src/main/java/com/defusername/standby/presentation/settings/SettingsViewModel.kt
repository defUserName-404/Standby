package com.defusername.standby.presentation.settings

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defusername.standby.domain.model.AppSettings
import com.defusername.standby.domain.model.PermissionState
import com.defusername.standby.domain.model.TriggerMode
import com.defusername.standby.domain.repository.PermissionRepository
import com.defusername.standby.domain.repository.SettingsRepository
import com.defusername.standby.domain.widget.WidgetSpec
import com.defusername.standby.presentation.widget.WidgetRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject

/** A user-togglable installed app for the exclude-list picker. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val permissionRepository: PermissionRepository,
    private val widgetRegistry: WidgetRegistry
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val permissionStates: StateFlow<List<PermissionState>> = permissionRepository.states

    /** Every registered widget, in stable registry order. */
    val widgetSpecs: List<WidgetSpec> = widgetRegistry.specs

    fun refreshPermissions() = permissionRepository.refresh()

    fun setTriggerMode(mode: TriggerMode) {
        update { it.copy(triggerMode = mode) }
    }

    fun setZenMode(enabled: Boolean) {
        update { it.copy(zenModeEnabled = enabled) }
    }

    fun setPackageExcluded(packageName: String, excluded: Boolean) {
        update { current ->
            val next = if (excluded) current.excludedPackages + packageName
            else current.excludedPackages - packageName
            current.copy(excludedPackages = next)
        }
    }

    /** Enable/disable a widget, preserving display order (requirement 9.2, 10.1). */
    fun setWidgetEnabled(id: String, enabled: Boolean) {
        update { current ->
            val effective = effectiveWidgetOrder(current)
            val next = if (enabled) effective + id else effective - id
            current.copy(enabledWidgetIds = next)
        }
    }

    /** Move a widget one step toward the front (towardFront=true) or back in display order. */
    fun moveWidget(id: String, towardFront: Boolean) {
        update { current ->
            val effective = effectiveWidgetOrder(current).toMutableList()
            val index = effective.indexOf(id)
            if (index >= 0) {
                val target = if (towardFront) index - 1 else index + 1
                if (target in effective.indices) {
                    Collections.swap(effective, index, target)
                }
            }
            current.copy(enabledWidgetIds = effective)
        }
    }

    /** The persisted order, or all widgets in registry order when unset (empty = all). */
    private fun effectiveWidgetOrder(current: AppSettings): List<String> =
        current.enabledWidgetIds.ifEmpty { widgetRegistry.specs.map { it.id } }

    /** Installed, launchable apps sorted by label, for the exclude-list picker. */
    fun loadInstalledApps(): List<InstalledApp> {
        val pm = context.packageManager
        val ownPackage = context.packageName
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != ownPackage && hasLauncherIntent(pm, it) }
            .map {
                InstalledApp(
                    packageName = it.packageName,
                    label = pm.getApplicationLabel(it).toString(),
                    isSystem = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun hasLauncherIntent(pm: PackageManager, info: ApplicationInfo): Boolean =
        pm.getLaunchIntentForPackage(info.packageName) != null

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepository.update(transform)
        }
    }
}
