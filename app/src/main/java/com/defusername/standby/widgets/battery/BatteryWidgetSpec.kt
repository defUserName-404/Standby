package com.defusername.standby.widgets.battery

import com.defusername.standby.domain.widget.WidgetSizeClass
import com.defusername.standby.domain.widget.WidgetSpec

object BatteryWidgetSpec : WidgetSpec {
    override val id: String = "battery"
    override val displayName: String = "Battery"
    override val sizeClass: WidgetSizeClass = WidgetSizeClass.SMALL
}
