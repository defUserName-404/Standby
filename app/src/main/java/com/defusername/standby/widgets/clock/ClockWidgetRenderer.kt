package com.defusername.standby.widgets.clock

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.defusername.standby.presentation.widget.WidgetRenderer

object ClockWidgetRenderer : WidgetRenderer {

    @Composable
    override fun render(state: Any?) {
        val time = state as? String
        Text(
            text = time ?: "--:--",
            style = TextStyle(color = Color.White, fontSize = 72.sp)
        )
    }
}
