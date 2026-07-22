package com.defusername.standby.widgets.notification

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defusername.standby.domain.model.NotificationEntry
import com.defusername.standby.presentation.widget.WidgetRenderer

object NotificationWidgetRenderer : WidgetRenderer {

    @Composable
    override fun render(state: Any?) {
        val entry = state as? NotificationEntry ?: return // nothing to show (zen / excluded / empty)
        val text = buildString {
            append(entry.appLabel)
            if (entry.title.isNotBlank()) append(": ").append(entry.title)
            if (entry.text.isNotBlank()) append(" — ").append(entry.text)
        }
        Text(
            text = text,
            style = TextStyle(color = Color.White, fontSize = 18.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
