package com.defusername.standby.widgets.notification

import com.defusername.standby.domain.widget.WidgetSizeClass
import com.defusername.standby.domain.widget.WidgetSpec

object NotificationWidgetSpec : WidgetSpec {
    override val id: String = "notification"
    override val displayName: String = "Latest notification"
    override val sizeClass: WidgetSizeClass = WidgetSizeClass.MEDIUM
}
