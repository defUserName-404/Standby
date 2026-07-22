package com.defusername.standby.data.local

import com.defusername.standby.domain.model.AppSettings
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Forward-migrates a persisted settings JSON blob to [AppSettings.CURRENT_SCHEMA_VERSION].
 * Each entry upgrades the document by exactly one version; add a new entry whenever
 * [AppSettings.CURRENT_SCHEMA_VERSION] is bumped. Migrations must preserve
 * user-configured values wherever a mapping exists (requirement 10.3).
 */
object SettingsMigration {

    fun migrate(json: JsonObject): JsonObject {
        var current = json
        var version = current["schemaVersion"]?.jsonPrimitive?.intOrNull ?: 1
        while (version < AppSettings.CURRENT_SCHEMA_VERSION) {
            val step = MIGRATIONS[version]
                ?: error("No settings migration from schema version $version")
            current = step(current)
            version++
        }
        return current
    }

    private val MIGRATIONS: Map<Int, (JsonObject) -> JsonObject> = mapOf(
        // v1 -> v2: adds onboardingCompleted (false = show onboarding on next launch).
        1 to { json ->
            JsonObject(
                json + mapOf(
                    "schemaVersion" to JsonPrimitive(2),
                    "onboardingCompleted" to JsonPrimitive(false)
                )
            )
        },
        // v2 -> v3: adds zenModeEnabled and excludedPackages (both defaulted).
        2 to { json ->
            JsonObject(
                json + mapOf(
                    "schemaVersion" to JsonPrimitive(3),
                    "zenModeEnabled" to JsonPrimitive(false),
                    "excludedPackages" to JsonArray(emptyList())
                )
            )
        }
    )
}
