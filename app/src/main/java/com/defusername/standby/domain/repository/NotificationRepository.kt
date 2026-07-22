package com.defusername.standby.domain.repository

import com.defusername.standby.domain.model.NotificationEntry
import kotlinx.coroutines.flow.StateFlow

interface NotificationRepository {

    /** Current notifications, most-recent first. */
    val notifications: StateFlow<List<NotificationEntry>>

    /** Insert or replace by [NotificationEntry.id]; keeps the list newest-first. */
    fun onPosted(entry: NotificationEntry)

    /** Drop the notification with the given id, if present. */
    fun onRemoved(id: String)
}
