package com.defusername.standby.widgets.battery

import com.defusername.standby.presentation.widget.WidgetDefinition
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object BatteryWidgetModule {

    @Provides
    @IntoSet
    fun provideBatteryWidget(
        dataSource: BatteryWidgetDataSource
    ): WidgetDefinition =
        WidgetDefinition(
            spec = BatteryWidgetSpec,
            dataSource = dataSource,
            renderer = BatteryWidgetRenderer
        )
}
