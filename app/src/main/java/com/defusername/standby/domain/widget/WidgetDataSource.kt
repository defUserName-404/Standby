package com.defusername.standby.domain.widget

import kotlinx.coroutines.flow.Flow

/**
 * Supplies the state a widget renders. The type parameter [T] is the widget's own
 * state shape (e.g. a clock's current time, a battery's percentage). The presentation
 * layer collects [data] and feeds it to the widget's Composable renderer.
 *
 * A widget whose data source throws is rendered in a safe empty/error state by the
 * StandBy screen without crashing sibling widgets (requirement 9.4) — error isolation
 * is applied at the collection site, not here.
 */
interface WidgetDataSource<T> {
    val data: Flow<T>
}
