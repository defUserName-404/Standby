package com.defusername.standby.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.defusername.standby.domain.model.PermissionState
import com.defusername.standby.domain.model.PermissionStatus
import com.defusername.standby.domain.model.PermissionType
import com.defusername.standby.domain.repository.PermissionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionRepository {

    private val _states = MutableStateFlow(checkAll())
    override val states: StateFlow<List<PermissionState>> = _states.asStateFlow()

    override fun refresh() {
        _states.value = checkAll()
    }

    private fun checkAll(): List<PermissionState> = listOf(
        PermissionState(PermissionType.POST_NOTIFICATIONS, postNotificationsStatus()),
        PermissionState(PermissionType.NOTIFICATION_LISTENER, notificationListenerStatus()),
        PermissionState(PermissionType.BATTERY_OPTIMIZATION_EXEMPTION, batteryExemptionStatus())
    )

    private fun postNotificationsStatus(): PermissionStatus = when {
        // Runtime permission only exists on API 33+; below that it is effectively granted.
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> PermissionStatus.GRANTED
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED -> PermissionStatus.GRANTED
        else -> PermissionStatus.DENIED
    }

    private fun notificationListenerStatus(): PermissionStatus =
        if (NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.SPECIAL_ACCESS_REQUIRED
        }

    private fun batteryExemptionStatus(): PermissionStatus {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (pm.isIgnoringBatteryOptimizations(context.packageName)) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.SPECIAL_ACCESS_REQUIRED
        }
    }
}
