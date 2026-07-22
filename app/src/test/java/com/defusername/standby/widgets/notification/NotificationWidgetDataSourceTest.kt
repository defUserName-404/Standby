package com.defusername.standby.widgets.notification

import com.defusername.standby.domain.model.AppSettings
import com.defusername.standby.domain.model.IconRef
import com.defusername.standby.domain.model.NotificationEntry
import com.defusername.standby.domain.repository.NotificationRepository
import com.defusername.standby.domain.repository.SettingsRepository
import com.defusername.standby.domain.usecase.NotificationFilterUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class NotificationWidgetDataSourceTest {

    private val notificationsFlow = MutableStateFlow<List<NotificationEntry>>(emptyList())
    private val settingsFlow = MutableStateFlow(AppSettings())

    private val dataSource = NotificationWidgetDataSource(
        notificationRepository = object : NotificationRepository {
            override val notifications = notificationsFlow
            override fun onPosted(entry: NotificationEntry) {}
            override fun onRemoved(id: String) {}
        },
        settingsRepository = object : SettingsRepository {
            override val settings: Flow<AppSettings> = settingsFlow
            override suspend fun update(transform: (AppSettings) -> AppSettings) {
                settingsFlow.value = transform(settingsFlow.value)
            }
        },
        notificationFilterUseCase = NotificationFilterUseCase()
    )

    @Test
    fun `emits the most recent non-excluded notification`() = runTest {
        notificationsFlow.value = listOf(entry("a", t(1)), entry("b", t(2)))
        assertEquals("b", dataSource.data.first()?.packageName)
    }

    @Test
    fun `zen mode on emits null`() = runTest {
        notificationsFlow.value = listOf(entry("a", t(1)))
        settingsFlow.value = AppSettings(zenModeEnabled = true)
        assertNull(dataSource.data.first())
    }

    @Test
    fun `excluded package emits null when it is the only one`() = runTest {
        notificationsFlow.value = listOf(entry("spam", t(1)))
        settingsFlow.value = AppSettings(excludedPackages = setOf("spam"))
        assertNull(dataSource.data.first())
    }

    @Test
    fun `emits null when there are no notifications`() = runTest {
        notificationsFlow.value = emptyList()
        assertNull(dataSource.data.first())
    }

    private fun t(seconds: Long): Instant = Instant.ofEpochSecond(1_700_000_000L + seconds)

    private fun entry(packageName: String, postedAt: Instant): NotificationEntry =
        NotificationEntry(
            id = "$packageName:1",
            packageName = packageName,
            appLabel = packageName,
            iconRef = IconRef(packageName, 0),
            title = "title",
            text = "",
            postedAt = postedAt,
            isSensitive = false
        )
}
