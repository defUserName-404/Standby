package com.defusername.standby.widgets.clock

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockWidgetDataSourceTest {

    private val dataSource = ClockWidgetDataSource()

    @Test
    fun `first emission is a formatted HH mm string`() = runTest {
        val time = dataSource.data.first()
        assertTrue("expected HH:mm, got '$time'", time.matches(Regex("\\d{2}:\\d{2}")))
    }
}
