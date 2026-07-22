package com.defusername.standby.widgets.notification

import com.defusername.standby.domain.model.NotificationEntry
import com.defusername.standby.domain.repository.NotificationRepository
import com.defusername.standby.domain.repository.SettingsRepository
import com.defusername.standby.domain.usecase.NotificationFilterUseCase
import com.defusername.standby.domain.widget.WidgetDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * The displayable notification: the single most-recent non-excluded entry when zen mode
 * is off, or null when zen is on / everything is excluded / nothing is posted. Combines
 * the raw notification stream with settings so zen/exclude changes propagate live (5.3).
 */
class NotificationWidgetDataSource @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationFilterUseCase: NotificationFilterUseCase
) : WidgetDataSource<NotificationEntry?> {

    override val data: Flow<NotificationEntry?> = combine(
        notificationRepository.notifications,
        settingsRepository.settings
    ) { list, settings ->
        notificationFilterUseCase.filter(list, settings.zenModeEnabled, settings.excludedPackages)
    }
}
