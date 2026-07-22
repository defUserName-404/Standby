package com.defusername.standby.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Stub notification listener — exists so the app appears in
 * Settings → Notification access and the onboarding deep-link has a target.
 * Real capture/mapping is wired up in the notification-pipeline phase (tasks 15-16).
 */
class StandByNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        Log.i(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        Log.w(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // No-op until the NotificationMapper/NotificationRepository pipeline lands.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No-op until the NotificationMapper/NotificationRepository pipeline lands.
    }

    private companion object {
        const val TAG = "NotifListenerService"
    }
}
