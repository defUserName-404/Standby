package com.defusername.standby.data.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SCREEN_OFF
import android.content.Intent.ACTION_SCREEN_ON
import android.content.Intent.ACTION_USER_PRESENT
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import com.defusername.standby.domain.model.PowerState

class ScreenPowerReceiver(
    private val onUpdate: (PowerState) -> Unit
) : BroadcastReceiver() {

    private var currentState = PowerState()
    private var initialized = false

    override fun onReceive(context: Context, intent: Intent) {
        if (!initialized) {
            seedFromSystem(context)
            initialized = true
            Log.d(TAG, "Seeded from system: $currentState")
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = pm.isInteractive

        when (intent.action) {
            ACTION_SCREEN_ON -> {
                currentState = currentState.copy(isScreenOn = true)
            }
            ACTION_SCREEN_OFF -> {
                currentState = currentState.copy(isScreenOn = false)
            }
            ACTION_USER_PRESENT -> {
                // Tracked for future use by TriggerEvaluatorUseCase
            }
            Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED -> {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                val isCharging = intent.action == Intent.ACTION_POWER_CONNECTED
                currentState = currentState.copy(
                    isCharging = isCharging,
                    batteryPercent = batteryLevel
                )
            }
        }

        // Also snapshot screen state in case we missed a broadcast
        currentState = currentState.copy(isScreenOn = isScreenOn)

        Log.d(TAG, "Action: ${intent.action} → $currentState")
        onUpdate(currentState)
    }

    private fun seedFromSystem(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        currentState = PowerState(
            isScreenOn = pm.isInteractive,
            isCharging = bm.isCharging,
            batteryPercent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        )
    }

    companion object {
        private const val TAG = "StandBy"
    }
}
