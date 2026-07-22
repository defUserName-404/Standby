package com.defusername.standby.widgets.notification

import com.defusername.standby.presentation.widget.WidgetDefinition
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object NotificationWidgetModule {

    @Provides
    @IntoSet
    fun provideNotificationWidget(
        dataSource: NotificationWidgetDataSource
    ): WidgetDefinition =
        WidgetDefinition(
            spec = NotificationWidgetSpec,
            dataSource = dataSource,
            renderer = NotificationWidgetRenderer
        )
}
