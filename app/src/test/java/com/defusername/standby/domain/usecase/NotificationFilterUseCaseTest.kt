package com.defusername.standby.domain.usecase

import com.defusername.standby.domain.model.IconRef
import com.defusername.standby.domain.model.NotificationEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class NotificationFilterUseCaseTest {

    private val useCase = NotificationFilterUseCase()

    @Test
    fun `zen enabled returns null even when notifications exist`() {
        val result = useCase.filter(listOf(entry("a", t(2))), zenEnabled = true, excludedPackages = emptySet())
        assertNull(result)
    }

    @Test
    fun `zen disabled and empty list returns null`() {
        assertNull(useCase.filter(emptyList(), zenEnabled = false, excludedPackages = emptySet()))
    }

    @Test
    fun `zen disabled returns the single most recent non-excluded notification`() {
        val older = entry("a", t(1))
        val newer = entry("b", t(2))

        val result = useCase.filter(listOf(older, newer), zenEnabled = false, excludedPackages = emptySet())

        assertEquals(newer, result)
    }

    @Test
    fun `excluded packages are never shown`() {
        val excluded = entry("excluded", t(5))
        val included = entry("included", t(1))

        val result = useCase.filter(
            listOf(excluded, included),
            zenEnabled = false,
            excludedPackages = setOf("excluded")
        )

        assertEquals(included, result)
    }

    @Test
    fun `all excluded returns null`() {
        val list = listOf(entry("a", t(1)), entry("b", t(2)))

        assertNull(
            useCase.filter(list, zenEnabled = false, excludedPackages = setOf("a", "b"))
        )
    }

    @Test
    fun `most recent is chosen regardless of input order`() {
        val newest = entry("c", t(3))
        val middle = entry("b", t(2))
        val oldest = entry("a", t(1))

        val result = useCase.filter(listOf(oldest, newest, middle), zenEnabled = false, excludedPackages = emptySet())

        assertEquals(newest, result)
    }

    private fun t(seconds: Long): Instant = Instant.ofEpochSecond(1_700_000_000L + seconds)

    private fun entry(packageName: String, postedAt: Instant, title: String = "t"): NotificationEntry =
        NotificationEntry(
            id = "$packageName:1",
            packageName = packageName,
            appLabel = packageName,
            iconRef = IconRef(packageName, 0),
            title = title,
            text = "",
            postedAt = postedAt,
            isSensitive = false
        )
}
