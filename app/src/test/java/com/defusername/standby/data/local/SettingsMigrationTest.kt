package com.defusername.standby.data.local

import com.defusername.standby.domain.model.AppSettings
import com.defusername.standby.domain.model.TriggerMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsMigrationTest {

    // Mirrors the Json configuration used by SettingsRepositoryImpl.
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `v1 fixture migrates to current schema and preserves user values`() {
        val v1Fixture = """
            {"schemaVersion":1,"triggerMode":"CHARGING_ONLY"}
        """.trimIndent()

        val migrated = SettingsMigration.migrate(json.parseToJsonElement(v1Fixture).jsonObject)

        assertEquals(
            AppSettings.CURRENT_SCHEMA_VERSION,
            migrated["schemaVersion"]?.jsonPrimitive?.intOrNull
        )
        assertEquals(false, migrated["onboardingCompleted"]?.jsonPrimitive?.booleanOrNull)

        val settings = json.decodeFromJsonElement(AppSettings.serializer(), migrated)
        assertEquals(TriggerMode.CHARGING_ONLY, settings.triggerMode)
        assertEquals(false, settings.onboardingCompleted)
        assertEquals(AppSettings.CURRENT_SCHEMA_VERSION, settings.schemaVersion)
    }

    @Test
    fun `current-schema document passes through unchanged`() {
        val current = json.encodeToString(
            AppSettings.serializer(),
            AppSettings(onboardingCompleted = true, triggerMode = TriggerMode.ALWAYS)
        )

        val migrated = SettingsMigration.migrate(json.parseToJsonElement(current).jsonObject)
        val settings = json.decodeFromJsonElement(AppSettings.serializer(), migrated)

        assertEquals(true, settings.onboardingCompleted)
        assertEquals(TriggerMode.ALWAYS, settings.triggerMode)
        assertEquals(AppSettings.CURRENT_SCHEMA_VERSION, settings.schemaVersion)
    }

    @Test
    fun `v2 fixture migrates to current schema and preserves user values`() {
        val v2Fixture = """
            {"schemaVersion":2,"onboardingCompleted":true,"triggerMode":"CHARGING_ONLY"}
        """.trimIndent()

        val migrated = SettingsMigration.migrate(json.parseToJsonElement(v2Fixture).jsonObject)
        val settings = json.decodeFromJsonElement(AppSettings.serializer(), migrated)

        assertEquals(AppSettings.CURRENT_SCHEMA_VERSION, settings.schemaVersion)
        assertEquals(true, settings.onboardingCompleted)
        assertEquals(TriggerMode.CHARGING_ONLY, settings.triggerMode)
        assertEquals(false, settings.zenModeEnabled)
        assertTrue(settings.excludedPackages.isEmpty())
    }

    @Test
    fun `v3 fixture migrates to current schema and preserves user values`() {
        val v3Fixture = """
            {"schemaVersion":3,"onboardingCompleted":true,"triggerMode":"CHARGING_ONLY","zenModeEnabled":true,"excludedPackages":["com.foo"]}
        """.trimIndent()

        val migrated = SettingsMigration.migrate(json.parseToJsonElement(v3Fixture).jsonObject)
        val settings = json.decodeFromJsonElement(AppSettings.serializer(), migrated)

        assertEquals(AppSettings.CURRENT_SCHEMA_VERSION, settings.schemaVersion)
        assertEquals(true, settings.onboardingCompleted)
        assertEquals(TriggerMode.CHARGING_ONLY, settings.triggerMode)
        assertEquals(true, settings.zenModeEnabled)
        assertEquals(setOf("com.foo"), settings.excludedPackages)
        assertTrue(settings.enabledWidgetIds.isEmpty())
    }

    @Test
    fun `v1 fixture migrates through every step to current schema`() {
        val v1Fixture = """{"schemaVersion":1,"triggerMode":"ALWAYS"}"""

        val migrated = SettingsMigration.migrate(json.parseToJsonElement(v1Fixture).jsonObject)
        val settings = json.decodeFromJsonElement(AppSettings.serializer(), migrated)

        assertEquals(AppSettings.CURRENT_SCHEMA_VERSION, settings.schemaVersion)
        assertEquals(false, settings.onboardingCompleted)
        assertEquals(false, settings.zenModeEnabled)
        assertTrue(settings.excludedPackages.isEmpty())
    }

    @Test
    fun `document without schemaVersion is treated as v1 and migrated`() {
        val legacy = """{"triggerMode":"ALWAYS"}"""

        val migrated = SettingsMigration.migrate(json.parseToJsonElement(legacy).jsonObject)
        val settings = json.decodeFromJsonElement(AppSettings.serializer(), migrated)

        assertEquals(AppSettings.CURRENT_SCHEMA_VERSION, settings.schemaVersion)
        assertEquals(TriggerMode.ALWAYS, settings.triggerMode)
        assertEquals(false, settings.onboardingCompleted)
    }
}
