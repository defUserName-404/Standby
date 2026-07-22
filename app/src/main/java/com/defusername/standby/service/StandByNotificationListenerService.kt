package com.defusername.standby.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.defusername.standby.data.notification.NotificationMapper
import com.defusername.standby.domain.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class StandByNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var notificationMapper: NotificationMapper

    @Inject
    lateinit var notificationRepository: NotificationRepository

    override fun onListenerConnected() {
        Log.i(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        Log.w(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val entry = sbn?.let(notificationMapper::map) ?: return
        notificationRepository.onPosted(entry)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val entry = sbn?.let(notificationMapper::map) ?: return
        notificationRepository.onRemoved(entry.id)
    }

    private companion object {
        const val TAG = "NotifListenerService"
    }
}
