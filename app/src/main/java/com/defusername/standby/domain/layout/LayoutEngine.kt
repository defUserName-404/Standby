package com.defusername.standby.domain.layout

import com.defusername.standby.domain.widget.WidgetSizeClass
import com.defusername.standby.domain.widget.WidgetSpec
import javax.inject.Inject

enum class Orientation {
    PORTRAIT,
    LANDSCAPE
}

/** The ordered widget ids to render, plus the orientation the order was computed for. */
data class WidgetArrangement(
    val orientation: Orientation,
    val orderedIds: List<String>
)

/**
 * Pure layout function (requirements 6.4, 9.3).
 *
 *  - 9.3: widgets are ordered by their declared [WidgetSpec.sizeClass].
 *  - 6.4: the order changes with [Orientation] so a rotation re-arranges the screen.
 *
 * Portrait stacks full-width elements first (clock on top), then medium, then small.
 * Landscape leads with compact small elements, then full-width, then medium. Ties
 * preserve the input (enable) order via a stable sort.
 */
class LayoutEngine @Inject constructor() {

    fun arrange(specs: List<WidgetSpec>, orientation: Orientation): WidgetArrangement {
        val comparator: Comparator<WidgetSpec> = when (orientation) {
            Orientation.PORTRAIT -> compareBy { portraitPriority(it.sizeClass) }
            Orientation.LANDSCAPE -> compareBy { landscapePriority(it.sizeClass) }
        }
        val ordered = specs.sortedWith(comparator)
        return WidgetArrangement(orientation, ordered.map { it.id })
    }

    private fun portraitPriority(sizeClass: WidgetSizeClass): Int = when (sizeClass) {
        WidgetSizeClass.FULL_WIDTH -> 0
        WidgetSizeClass.MEDIUM -> 1
        WidgetSizeClass.SMALL -> 2
    }

    private fun landscapePriority(sizeClass: WidgetSizeClass): Int = when (sizeClass) {
        WidgetSizeClass.SMALL -> 0
        WidgetSizeClass.FULL_WIDTH -> 1
        WidgetSizeClass.MEDIUM -> 2
    }
}
