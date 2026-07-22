package com.defusername.standby.data.notification

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import com.defusername.standby.domain.model.IconRef
import com.defusername.standby.domain.model.NotificationEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationMapper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Maps a system notification to the domain model. Returns null for notifications
     * that should never enter the pipeline — currently only our own (e.g. the monitor
     * service's persistent notification, which is not user-facing content).
     */
    fun map(sbn: StatusBarNotification): NotificationEntry? {
        if (sbn.packageName == context.packageName) return null

        return NotificationEntry(
            id = "${sbn.packageName}:${sbn.key}",
            packageName = sbn.packageName,
            appLabel = resolveAppLabel(sbn.packageName),
            iconRef = resolveIconRef(sbn),
            title = sbn.notification.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            text = sbn.notification.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
            postedAt = Instant.ofEpochMilli(sbn.postTime),
            isSensitive = sbn.notification.visibility == Notification.VISIBILITY_SECRET
        )
    }

    private fun resolveAppLabel(packageName: String): String = try {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName
    }

    private fun resolveIconRef(sbn: StatusBarNotification): IconRef {
        val icon = sbn.notification.smallIcon ?: return IconRef(sbn.packageName, 0)
        return if (icon.type == android.graphics.drawable.Icon.TYPE_RESOURCE) {
            IconRef(sbn.packageName, icon.resId)
        } else {
            IconRef(sbn.packageName, 0)
        }
    }
}
