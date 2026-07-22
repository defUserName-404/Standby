package com.defusername.standby.presentation.widget

import androidx.compose.runtime.Composable

/**
 * Draws a widget on the StandBy screen. [state] is the widget's own state shape
 * (or null for an empty/error state — requirement 9.4); the implementation casts
 * it to its expected type. This pairing is always safe because a widget's module
 * binds its own [com.defusername.standby.domain.widget.WidgetDataSource] and
 * [WidgetRenderer] together.
 */
interface WidgetRenderer {
    @Composable
    fun render(state: Any?)
}
