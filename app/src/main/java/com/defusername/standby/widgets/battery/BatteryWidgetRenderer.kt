package com.defusername.standby.widgets.battery

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.defusername.standby.presentation.widget.WidgetRenderer

object BatteryWidgetRenderer : WidgetRenderer {

    @Composable
    override fun render(state: Any?) {
        val percent = state as? Int ?: return
        Text(
            text = "$percent%",
            style = TextStyle(color = Color.White, fontSize = 24.sp)
        )
    }
}
