package com.defusername.standby.presentation.onboarding

import android.os.Build

/**
 * Best-effort guidance for OEMs with aggressive background-kill behavior
 * (requirement 12.3 mitigation). These steps are manual by nature — the OEM
 * settings screens have no stable public intents — so we show instructions.
 */
object OemGuidance {

    private val aggressiveManufacturers = setOf(
        "xiaomi", "redmi", "poco",
        "samsung",
        "oppo", "realme", "oneplus",
        "vivo", "iqoo",
        "huawei", "honor",
        "meizu", "asus", "tecno", "infinix", "itel"
    )

    val isAggressiveOem: Boolean
        get() = Build.MANUFACTURER.lowercase() in aggressiveManufacturers

    val manufacturerDisplayName: String
        get() = Build.MANUFACTURER.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }

    fun instructions(): List<String> = when (Build.MANUFACTURER.lowercase()) {
        "xiaomi", "redmi", "poco" -> listOf(
            "Settings → Apps → Manage apps → StandBy → enable \"Autostart\".",
            "In the same screen, set \"Battery saver\" to \"No restrictions\".",
            "Optionally lock StandBy in the recents view."
        )
        "samsung" -> listOf(
            "Settings → Battery → Background usage limits → remove StandBy from \"Sleeping apps\" and \"Deep sleeping apps\".",
            "Settings → Apps → StandBy → Battery → select \"Unrestricted\"."
        )
        "huawei", "honor" -> listOf(
            "Settings → Apps → StandBy → Battery → choose \"Launch manually\" and enable all three toggles (auto-launch, secondary launch, run in background)."
        )
        "oppo", "realme", "oneplus" -> listOf(
            "Settings → Battery → App battery management → StandBy → \"Don't optimize\".",
            "Lock StandBy in the recents view."
        )
        "vivo", "iqoo" -> listOf(
            "Settings → Battery → Background power consumption management → allow StandBy to run in the background.",
            "Enable \"Autostart\" for StandBy in app management."
        )
        else -> listOf(
            "Open StandBy's app settings and set battery usage to \"Unrestricted\".",
            "If your system offers an \"Autostart\" or \"Run in background\" toggle for apps, enable it for StandBy."
        )
    }
}
