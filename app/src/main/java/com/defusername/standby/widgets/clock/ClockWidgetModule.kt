package com.defusername.standby.widgets.clock

import com.defusername.standby.presentation.widget.WidgetDefinition
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object ClockWidgetModule {

    @Provides
    @IntoSet
    fun provideClockWidget(dataSource: ClockWidgetDataSource): WidgetDefinition =
        WidgetDefinition(
            spec = ClockWidgetSpec,
            dataSource = dataSource,
            renderer = ClockWidgetRenderer
        )
}
