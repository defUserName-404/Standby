package com.defusername.standby.data.notification

import com.defusername.standby.domain.model.NotificationEntry
import com.defusername.standby.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor() : NotificationRepository {

    private val _notifications = MutableStateFlow<List<NotificationEntry>>(emptyList())
    override val notifications: StateFlow<List<NotificationEntry>> = _notifications.asStateFlow()

    override fun onPosted(entry: NotificationEntry) {
        _notifications.value = rebuild(
            (_notifications.value.filter { it.id != entry.id } + entry)
                .sortedByDescending { it.postedAt }
        )
    }

    override fun onRemoved(id: String) {
        _notifications.value = _notifications.value.filter { it.id != id }
    }

    private fun rebuild(list: List<NotificationEntry>): List<NotificationEntry> =
        if (list.size > MAX_ENTRIES) list.take(MAX_ENTRIES) else list

    private companion object {
        const val MAX_ENTRIES = 50
    }
}
