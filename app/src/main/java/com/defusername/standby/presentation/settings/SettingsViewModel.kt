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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val permissionRepository: PermissionRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val permissionStates: StateFlow<List<PermissionState>> = permissionRepository.states

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
