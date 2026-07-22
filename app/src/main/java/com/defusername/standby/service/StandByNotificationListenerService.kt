package com.defusername.standby.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.defusername.standby.data.notification.NotificationMapper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class StandByNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var notificationMapper: NotificationMapper

    override fun onListenerConnected() {
        Log.i(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        Log.w(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val entry = sbn?.let(notificationMapper::map) ?: return
        Log.d(TAG, "Notification posted: ${entry.appLabel} — ${entry.title}")
        // Forwarding to NotificationRepository lands in task 16.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val entry = sbn?.let(notificationMapper::map) ?: return
        Log.d(TAG, "Notification removed: ${entry.appLabel} — ${entry.title}")
        // Forwarding to NotificationRepository lands in task 16.
    }

    private companion object {
        const val TAG = "NotifListenerService"
    }
}
