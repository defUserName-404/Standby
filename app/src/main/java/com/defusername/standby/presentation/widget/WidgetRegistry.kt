package com.defusername.standby.presentation.widget

import com.defusername.standby.domain.widget.WidgetSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds every widget the app knows about, keyed by [WidgetSpec.id]. Assembled from the
 * Hilt `Set<WidgetDefinition>` multibinding; with no widget modules contributed yet the
 * set is empty (task 24 scope). Tasks 25-27 add widget modules without touching this class.
 */
@Singleton
class WidgetRegistry @Inject constructor(
    definitions: Set<WidgetDefinition>
) {
    private val byId: Map<String, WidgetDefinition> =
        definitions.associateBy { it.spec.id }

    /** All known widget specs, stable-ordered by id. */
    val specs: List<WidgetSpec> = byId.keys.sorted().map { byId.getValue(it).spec }

    fun definition(id: String): WidgetDefinition? = byId[id]

    fun spec(id: String): WidgetSpec? = byId[id]?.spec

    fun contains(id: String): Boolean = byId.containsKey(id)
}
