package com.defusername.standby.presentation.standby

import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defusername.standby.data.SessionStateHolder
import com.defusername.standby.domain.repository.NotificationRepository
import com.defusername.standby.domain.repository.PowerStateRepository
import com.defusername.standby.domain.repository.SettingsRepository
import com.defusername.standby.domain.usecase.NotificationFilterUseCase
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

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var notificationFilterUseCase: NotificationFilterUseCase

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        acquireWakeLock()

        setContent {
            var timeText by remember { mutableStateOf(formatTime()) }
            val notifications by notificationRepository.notifications.collectAsStateWithLifecycle()
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            // Reactive: re-runs whenever notifications or settings (zen/excludes) change,
            // so toggling zen mode while StandBy is visible updates the display immediately (5.3).
            val latest = settings?.let { s ->
                notificationFilterUseCase.filter(
                    notifications, s.zenModeEnabled, s.excludedPackages
                )
            }

            LaunchedEffect(Unit) {
                while (true) {
                    timeText = formatTime()
                    delay(1000)
                }
            }

            LaunchedEffect(Unit) {
                var lastIsScreenOn = true
                powerStateRepository.state.collect { state ->
                    if (!lastIsScreenOn && state.isScreenOn) {
                        Log.d(TAG, "Screen turned on, finishing")
                        finish()
                    }
                    lastIsScreenOn = state.isScreenOn
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            Log.d(TAG, "Tap detected, finishing")
                            finish()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = timeText,
                        style = TextStyle(color = Color.White, fontSize = 72.sp)
                    )
                    if (latest != null) {
                        Text(
                            text = "${latest.appLabel}: ${latest.title}${if (latest.text.isNotBlank()) " — ${latest.text}" else ""}",
                            style = TextStyle(color = Color.White, fontSize = 18.sp),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 24.dp, start = 32.dp, end = 32.dp)
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d(TAG, "onWindowFocusChanged: hasFocus=$hasFocus")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        releaseWakeLock()
        sessionStateHolder.dismiss()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "StandBy::ActivityWakeLock"
            ).apply {
                acquire(10 * 60 * 1000L) // 10 minutes max
            }
            Log.d(TAG, "WakeLock acquired")
        } catch (e: SecurityException) {
            Log.e(TAG, "WAKE_LOCK permission not granted, continuing without WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }
        wakeLock = null
    }

    private fun formatTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    companion object {
        private const val TAG = "StandBy"
    }
}
