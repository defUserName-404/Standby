package com.defusername.standby.presentation.standby

import com.defusername.standby.domain.layout.LayoutEngine
import com.defusername.standby.domain.layout.Orientation
import com.defusername.standby.domain.model.AppSettings
import com.defusername.standby.domain.repository.SettingsRepository
import com.defusername.standby.domain.widget.WidgetDataSource
import com.defusername.standby.domain.widget.WidgetSizeClass
import com.defusername.standby.domain.widget.WidgetSpec
import com.defusername.standby.presentation.widget.WidgetDefinition
import com.defusername.standby.presentation.widget.WidgetRegistry
import com.defusername.standby.presentation.widget.WidgetRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalCoroutinesApi::class)
class StandByScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `only enabled widgets data sources are subscribed`() = runTest(testDispatcher) {
        val aCollected = AtomicBoolean(false)
        val bCollected = AtomicBoolean(false)
        val registry = registry(
            definition("a", RecordingDataSource("A", aCollected)),
            definition("b", RecordingDataSource("B", bCollected))
        )
        val settings = MutableStateFlow(AppSettings(enabledWidgetIds = listOf("a")))

        val vm = StandByScreenViewModel(registry, fakeSettings(settings), LayoutEngine())
        vm.uiState.filterNotNull().first()

        assertTrue("enabled widget should be subscribed", aCollected.get())
        assertFalse("disabled widget should NOT be subscribed", bCollected.get())
        assertEquals(setOf("a"), vm.uiState.value?.widgetStates?.keys)
    }

    @Test
    fun `empty enabled list enables all widgets`() = runTest(testDispatcher) {
        val aCollected = AtomicBoolean(false)
        val bCollected = AtomicBoolean(false)
        val registry = registry(
            definition("a", RecordingDataSource("A", aCollected)),
            definition("b", RecordingDataSource("B", bCollected))
        )
        val settings = MutableStateFlow(AppSettings(enabledWidgetIds = emptyList()))

        val vm = StandByScreenViewModel(registry, fakeSettings(settings), LayoutEngine())
        vm.uiState.filterNotNull().first()

        assertTrue(aCollected.get())
        assertTrue(bCollected.get())
        assertEquals(setOf("a", "b"), vm.uiState.value?.widgetStates?.keys)
    }

    @Test
    fun `throwing data source becomes null state without affecting siblings`() = runTest(testDispatcher) {
        val registry = registry(
            definition("a", ThrowingDataSource()),
            definition("b", RecordingDataSource("B", AtomicBoolean()))
        )
        val settings = MutableStateFlow(AppSettings(enabledWidgetIds = listOf("a", "b")))

        val vm = StandByScreenViewModel(registry, fakeSettings(settings), LayoutEngine())
        val state = vm.uiState.filterNotNull().first()

        assertNull("failing widget state should be null", state.widgetStates["a"])
        assertEquals("B", state.widgetStates["b"])
    }

    @Test
    fun `arrangement respects enabled widgets and orientation`() = runTest(testDispatcher) {
        val registry = registry(
            definition("clock", WidgetSizeClass.FULL_WIDTH, RecordingDataSource("c", AtomicBoolean())),
            definition("battery", WidgetSizeClass.SMALL, RecordingDataSource("bat", AtomicBoolean()))
        )
        val settings = MutableStateFlow(AppSettings(enabledWidgetIds = listOf("clock", "battery")))

        val vm = StandByScreenViewModel(registry, fakeSettings(settings), LayoutEngine())
        vm.setOrientation(Orientation.LANDSCAPE)
        val state = vm.uiState.filterNotNull().first()

        assertEquals(listOf("battery", "clock"), state.arrangement.orderedIds)
    }

    private fun registry(vararg defs: WidgetDefinition): WidgetRegistry =
        WidgetRegistry(defs.toSet())

    private fun definition(id: String, dataSource: WidgetDataSource<*>): WidgetDefinition =
        WidgetDefinition(spec(id, WidgetSizeClass.SMALL), dataSource, NoopRenderer)

    private fun definition(id: String, size: WidgetSizeClass, dataSource: WidgetDataSource<*>): WidgetDefinition =
        WidgetDefinition(spec(id, size), dataSource, NoopRenderer)

    private fun spec(id: String, size: WidgetSizeClass): WidgetSpec =
        object : WidgetSpec {
            override val id: String = id
            override val displayName: String = id
            override val sizeClass: WidgetSizeClass = size
        }

    private fun fakeSettings(state: MutableStateFlow<AppSettings>): SettingsRepository =
        object : SettingsRepository {
            override val settings: Flow<AppSettings> = state
            override suspend fun update(transform: (AppSettings) -> AppSettings) {
                state.value = transform(state.value)
            }
        }

    private object NoopRenderer : WidgetRenderer {
        @androidx.compose.runtime.Composable
        override fun render(state: Any?) { /* not invoked in tests */ }
    }

    private class RecordingDataSource(private val value: String, private val flag: AtomicBoolean) : WidgetDataSource<String> {
        override val data: Flow<String> = flow { flag.set(true); emit(value) }
    }

    private class ThrowingDataSource : WidgetDataSource<Unit> {
        override val data: Flow<Unit> = flow { throw RuntimeException("boom") }
    }
}
