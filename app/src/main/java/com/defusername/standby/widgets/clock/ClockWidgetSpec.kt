package com.defusername.standby.widgets.clock

import com.defusername.standby.domain.widget.WidgetSizeClass
import com.defusername.standby.domain.widget.WidgetSpec

object ClockWidgetSpec : WidgetSpec {
    override val id: String = "clock"
    override val displayName: String = "Clock"
    override val sizeClass: WidgetSizeClass = WidgetSizeClass.FULL_WIDTH
}
