package com.defusername.standby.domain.layout

import com.defusername.standby.domain.widget.WidgetSizeClass
import com.defusername.standby.domain.widget.WidgetSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LayoutEngineTest {

    private val engine = LayoutEngine()

    @Test
    fun `empty specs produce empty arrangement`() {
        val arrangement = engine.arrange(emptyList(), Orientation.PORTRAIT)
        assertEquals(emptyList<String>(), arrangement.orderedIds)
    }

    @Test
    fun `portrait orders full-width before medium before small`() {
        val specs = listOf(
            spec("battery", WidgetSizeClass.SMALL),
            spec("notif", WidgetSizeClass.MEDIUM),
            spec("clock", WidgetSizeClass.FULL_WIDTH)
        )

        val arrangement = engine.arrange(specs, Orientation.PORTRAIT)

        assertEquals(listOf("clock", "notif", "battery"), arrangement.orderedIds)
    }

    @Test
    fun `landscape orders small before full-width before medium`() {
        val specs = listOf(
            spec("notif", WidgetSizeClass.MEDIUM),
            spec("clock", WidgetSizeClass.FULL_WIDTH),
            spec("battery", WidgetSizeClass.SMALL)
        )

        val arrangement = engine.arrange(specs, Orientation.LANDSCAPE)

        assertEquals(listOf("battery", "clock", "notif"), arrangement.orderedIds)
    }

    @Test
    fun `orientation change re-arranges the same widgets`() {
        val specs = listOf(
            spec("clock", WidgetSizeClass.FULL_WIDTH),
            spec("battery", WidgetSizeClass.SMALL),
            spec("notif", WidgetSizeClass.MEDIUM)
        )

        val portrait = engine.arrange(specs, Orientation.PORTRAIT)
        val landscape = engine.arrange(specs, Orientation.LANDSCAPE)

        assertNotEquals(portrait.orderedIds, landscape.orderedIds)
        assertEquals(listOf("clock", "notif", "battery"), portrait.orderedIds)
        assertEquals(listOf("battery", "clock", "notif"), landscape.orderedIds)
    }

    @Test
    fun `ties preserve enable order`() {
        val specs = listOf(
            spec("a", WidgetSizeClass.SMALL),
            spec("b", WidgetSizeClass.SMALL),
            spec("c", WidgetSizeClass.SMALL)
        )

        val arrangement = engine.arrange(specs, Orientation.PORTRAIT)

        assertEquals(listOf("a", "b", "c"), arrangement.orderedIds)
    }

    private fun spec(id: String, sizeClass: WidgetSizeClass): WidgetSpec =
        object : WidgetSpec {
            override val id: String = id
            override val displayName: String = id
            override val sizeClass: WidgetSizeClass = sizeClass
        }
}
