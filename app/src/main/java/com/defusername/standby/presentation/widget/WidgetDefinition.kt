package com.defusername.standby.presentation.widget

import com.defusername.standby.domain.widget.WidgetDataSource
import com.defusername.standby.domain.widget.WidgetSpec

/**
 * One registered widget: its spec (identity + layout hint), its data source, and its
 * renderer. Assembled by Hilt multibinding (each widget's own module provides a
 * [WidgetDefinition] via `@IntoSet`), so adding a widget touches only that widget's
 * files plus this one registry entry point (requirement 9.1).
 */
data class WidgetDefinition(
    val spec: WidgetSpec,
    val dataSource: WidgetDataSource<*>,
    val renderer: WidgetRenderer
)
