package com.defusername.standby.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val onboardingCompleted: Boolean = false,
    val triggerMode: TriggerMode = TriggerMode.ALWAYS,
    val zenModeEnabled: Boolean = false,
    val excludedPackages: Set<String> = emptySet(),
    val enabledWidgetIds: List<String> = emptyList()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 4
    }
}

@Serializable
enum class TriggerMode {
    CHARGING_ONLY,
    ALWAYS
}
