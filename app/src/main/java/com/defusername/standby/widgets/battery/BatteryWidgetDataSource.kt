package com.defusername.standby.widgets.battery

import com.defusername.standby.domain.repository.PowerStateRepository
import com.defusername.standby.domain.widget.WidgetDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Current battery percentage (0-100) from the power-state stream. */
class BatteryWidgetDataSource @Inject constructor(
    private val powerStateRepository: PowerStateRepository
) : WidgetDataSource<Int> {

    override val data: Flow<Int> =
        powerStateRepository.state.map { it.batteryPercent }
}
