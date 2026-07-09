package com.defusername.standby.domain.model

data class PowerState(
    val isScreenOn: Boolean = false,
    val isCharging: Boolean = false,
    val batteryPercent: Int = 0
)
