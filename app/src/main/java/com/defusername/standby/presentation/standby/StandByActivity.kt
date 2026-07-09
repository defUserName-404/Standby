package com.defusername.standby.presentation.standby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.defusername.standby.data.SessionStateHolder
import com.defusername.standby.domain.repository.PowerStateRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class StandByActivity : ComponentActivity() {

    @Inject
    lateinit var sessionStateHolder: SessionStateHolder

    @Inject
    lateinit var powerStateRepository: PowerStateRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            var timeText by remember { mutableStateOf(formatTime()) }

            LaunchedEffect(Unit) {
                while (true) {
                    timeText = formatTime()
                    delay(1000)
                }
            }

            LaunchedEffect(Unit) {
                powerStateRepository.state.collect { state ->
                    if (!state.isScreenOn) {
                        finish()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures { finish() }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeText,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 72.sp
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        sessionStateHolder.dismiss()
        super.onDestroy()
    }

    private fun formatTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
}
