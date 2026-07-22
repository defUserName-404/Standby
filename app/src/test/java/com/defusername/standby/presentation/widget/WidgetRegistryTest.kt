package com.defusername.standby.presentation.widget

import com.defusername.standby.domain.widget.WidgetDataSource
import com.defusername.standby.domain.widget.WidgetSizeClass
import com.defusername.standby.domain.widget.WidgetSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetRegistryTest {

    @Test
    fun `empty registry has no specs and lookups return null`() {
        val registry = WidgetRegistry(emptySet())

        assertTrue(registry.specs.isEmpty())
        assertNull(registry.definition("anything"))
        assertFalse(registry.contains("anything"))
    }

    @Test
    fun `definitions are keyed by spec id and specs are sorted by id`() {
        val b = definition("b", "Bravo")
        val a = definition("a", "Alpha")

        val registry = WidgetRegistry(setOf(b, a))

        assertEquals(listOf("a", "b"), registry.specs.map { it.id })
        assertTrue(registry.contains("a"))
        assertNotNull(registry.definition("b"))
        assertEquals("Bravo", registry.spec("b")?.displayName)
    }

    @Test
    fun `lookup for unknown id returns null`() {
        val registry = WidgetRegistry(setOf(definition("a", "Alpha")))

        assertNull(registry.definition("zzz"))
        assertNull(registry.spec("zzz"))
        assertFalse(registry.contains("zzz"))
    }

    private fun definition(id: String, name: String): WidgetDefinition =
        WidgetDefinition(
            spec = FakeSpec(id, name),
            dataSource = FakeDataSource,
            renderer = NoopRenderer
        )

    private data class FakeSpec(override val id: String, override val displayName: String) : WidgetSpec {
        override val sizeClass: WidgetSizeClass = WidgetSizeClass.SMALL
    }

    private object FakeDataSource : WidgetDataSource<Unit> {
        override val data: Flow<Unit> = flowOf(Unit)
    }

    private object NoopRenderer : WidgetRenderer {
        @androidx.compose.runtime.Composable
        override fun render(state: Any?) { /* never invoked in tests */ }
    }
}
