package com.defusername.standby.widgets.clock

import com.defusername.standby.domain.widget.WidgetDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/** Emits the current time as "HH:mm" once per second. */
class ClockWidgetDataSource @Inject constructor() : WidgetDataSource<String> {

    override val data: Flow<String> = flow {
        while (true) {
            emit(formatTime())
            delay(1000)
        }
    }

    private fun formatTime(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}
