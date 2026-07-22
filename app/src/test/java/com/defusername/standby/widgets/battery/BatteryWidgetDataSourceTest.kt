package com.defusername.standby.widgets.battery

import com.defusername.standby.domain.model.PowerState
import com.defusername.standby.domain.repository.PowerStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryWidgetDataSourceTest {

    private val powerState = MutableStateFlow(PowerState(batteryPercent = 73))

    private val dataSource = BatteryWidgetDataSource(
        powerStateRepository = object : PowerStateRepository {
            override val state: StateFlow<PowerState> = powerState
        }
    )

    @Test
    fun `emits the current battery percentage`() = runTest {
        assertEquals(73, dataSource.data.first())
    }

    @Test
    fun `emits updated percentage when power state changes`() = runTest {
        powerState.value = PowerState(batteryPercent = 42)
        assertEquals(42, dataSource.data.first())
    }
}
