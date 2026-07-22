package com.defusername.standby.domain.widget

/**
 * Declares a widget's identity and layout needs. A widget is a self-contained unit
 * (spec + data source + Composable renderer) registered in the widget registry.
 *
 * Adding a widget must not require changes to the StandBy screen, the layout engine,
 * or any other widget (requirement 9.1).
 */
interface WidgetSpec {
    /** Stable unique id, persisted in `AppSettings.enabledWidgetIds`. */
    val id: String
    /** Human-readable name shown in the enable/disable + reorder UI. */
    val displayName: String
    /** Layout hint consumed by the layout engine. */
    val sizeClass: WidgetSizeClass
    /** Optional per-widget config fields; empty when the widget has no settings. */
    val settingsSchema: List<WidgetSettingField>
        get() = emptyList()
}

enum class WidgetSizeClass {
    SMALL,
    MEDIUM,
    FULL_WIDTH
}

/** A single configurable field for a widget, used to auto-generate per-widget settings UI. */
data class WidgetSettingField(
    val key: String,
    val label: String,
    val type: WidgetSettingType
)

enum class WidgetSettingType {
    BOOLEAN,
    STRING,
    INTEGER,
    COLOR
}
