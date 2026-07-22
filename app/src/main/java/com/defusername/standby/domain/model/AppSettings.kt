package com.defusername.standby.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val onboardingCompleted: Boolean = false,
    val triggerMode: TriggerMode = TriggerMode.ALWAYS
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}

@Serializable
enum class TriggerMode {
    CHARGING_ONLY,
    ALWAYS
}
