package com.defusername.standby.data.repository

import android.content.Context
import android.content.Intent.ACTION_SCREEN_OFF
import android.content.Intent.ACTION_SCREEN_ON
import android.content.Intent.ACTION_USER_PRESENT
import android.content.Intent.ACTION_POWER_CONNECTED
import android.content.Intent.ACTION_POWER_DISCONNECTED
import android.os.BatteryManager
import android.os.PowerManager
import com.defusername.standby.data.platform.ScreenPowerReceiver
import com.defusername.standby.domain.model.PowerState
import com.defusername.standby.domain.repository.PowerStateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PowerStateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PowerStateRepository {

    private val _state = MutableStateFlow(PowerState())
    override val state: StateFlow<PowerState> = _state.asStateFlow()

    private val receiver = ScreenPowerReceiver { powerState ->
        _state.value = powerState
    }

    private var registered = false

    fun registerReceiver() {
        if (registered) return
        val filter = android.content.IntentFilter().apply {
            addAction(ACTION_SCREEN_ON)
            addAction(ACTION_SCREEN_OFF)
            addAction(ACTION_USER_PRESENT)
            addAction(ACTION_POWER_CONNECTED)
            addAction(ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(receiver, filter)
        registered = true
        snapshotCurrentPowerState()
    }

    private fun snapshotCurrentPowerState() {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        _state.value = PowerState(
            isScreenOn = pm.isInteractive,
            isCharging = bm.isCharging,
            batteryPercent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        )
    }

    fun unregisterReceiver() {
        if (!registered) return
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered
        }
        registered = false
    }
}
