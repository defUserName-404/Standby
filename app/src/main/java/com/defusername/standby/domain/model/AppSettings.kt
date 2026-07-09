package com.defusername.standby.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val triggerMode: TriggerMode = TriggerMode.CHARGING_ONLY
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
enum class TriggerMode {
    CHARGING_ONLY,
    ALWAYS
}
