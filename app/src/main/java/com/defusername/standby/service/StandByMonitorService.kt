package com.defusername.standby.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.defusername.standby.R
import com.defusername.standby.data.notification.ListenerHealthMonitor
import com.defusername.standby.data.platform.StandByLauncher
import com.defusername.standby.data.repository.PowerStateRepositoryImpl
import com.defusername.standby.domain.model.PowerState
import com.defusername.standby.domain.usecase.TriggerEvaluatorUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StandByMonitorService : Service() {

    @Inject
    lateinit var powerStateRepository: PowerStateRepositoryImpl

    @Inject
    lateinit var triggerEvaluator: TriggerEvaluatorUseCase

    @Inject
    lateinit var standByLauncher: StandByLauncher

    @Inject
    lateinit var listenerHealthMonitor: ListenerHealthMonitor

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        powerStateRepository.registerReceiver()
        collectLaunchEvents()
        collectHealthChecks()
        collectRevocationPrompts()
        listenerHealthMonitor.check()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        powerStateRepository.unregisterReceiver()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun collectLaunchEvents() {
        serviceScope.launch {
            triggerEvaluator.launchEvents.collect {
                Log.d(TAG, "Launch event received, launching StandBy")
                standByLauncher.launch()
            }
        }
    }

    /** Re-check listener health on every screen-on transition (design.md §4). */
    private fun collectHealthChecks() {
        serviceScope.launch {
            var previousScreenOn = powerStateRepository.state.value.isScreenOn
            powerStateRepository.state.collect { state ->
                if (!previousScreenOn && state.isScreenOn) {
                    listenerHealthMonitor.check()
                }
                previousScreenOn = state.isScreenOn
            }
        }
    }

    /** Post / cancel the re-enable prompt based on [ListenerHealthMonitor.listenerRevoked]. */
    private fun collectRevocationPrompts() {
        serviceScope.launch {
            listenerHealthMonitor.listenerRevoked.collect { revoked ->
                if (revoked) showReEnablePrompt() else cancelReEnablePrompt()
            }
        }
    }

    private fun showReEnablePrompt() {
        ensureHealthChannel()
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, HEALTH_CHANNEL_ID)
            .setContentTitle(getString(R.string.listener_health_notification_title))
            .setContentText(getString(R.string.listener_health_notification_text))
            .setSmallIcon(R.drawable.ic_monitoring)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(HEALTH_NOTIFICATION_ID, notification)
    }

    private fun cancelReEnablePrompt() {
        getSystemService(NotificationManager::class.java)
            .cancel(HEALTH_NOTIFICATION_ID)
    }

    private fun ensureHealthChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(HEALTH_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    HEALTH_CHANNEL_ID,
                    getString(R.string.listener_health_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = getString(R.string.listener_health_channel_description)
                }
            )
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.monitor_service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.monitor_service_channel_description)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.monitor_service_notification_title))
        .setContentText(getString(R.string.monitor_service_notification_text))
        .setSmallIcon(R.drawable.ic_monitoring)
        .setOngoing(true)
        .setSilent(true)
        .build()

    companion object {
        private const val TAG = "StandBy"
        const val CHANNEL_ID = "standby_monitor"
        const val NOTIFICATION_ID = 1
        private const val HEALTH_CHANNEL_ID = "standby_listener_health"
        private const val HEALTH_NOTIFICATION_ID = 2
    }
}
