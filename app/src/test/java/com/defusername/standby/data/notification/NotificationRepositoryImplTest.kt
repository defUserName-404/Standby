package com.defusername.standby.data.notification

import com.defusername.standby.domain.model.IconRef
import com.defusername.standby.domain.model.NotificationEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NotificationRepositoryImplTest {

    private val repo = NotificationRepositoryImpl()

    @Test
    fun `onPosted adds and keeps newest first`() {
        val older = entry("a", Instant.parse("2024-01-01T00:00:00Z"))
        val newer = entry("b", Instant.parse("2024-01-02T00:00:00Z"))

        repo.onPosted(older)
        repo.onPosted(newer)

        assertEquals(listOf(newer, older), repo.notifications.value)
    }

    @Test
    fun `onPosted with existing id replaces in place`() {
        val original = entry("a", Instant.parse("2024-01-01T00:00:00Z"), title = "old")
        val updated = entry("a", Instant.parse("2024-01-03T00:00:00Z"), title = "new")

        repo.onPosted(original)
        repo.onPosted(updated)

        val list = repo.notifications.value
        assertEquals(1, list.size)
        assertEquals("new", list.first().title)
        assertEquals(updated, list.first())
    }

    @Test
    fun `onRemoved drops the matching id only`() {
        repo.onPosted(entry("a", Instant.parse("2024-01-01T00:00:00Z")))
        repo.onPosted(entry("b", Instant.parse("2024-01-02T00:00:00Z")))

        repo.onRemoved("a")

        assertEquals(listOf("b"), repo.notifications.value.map { it.id })
    }

    @Test
    fun `onRemoved for unknown id is a no-op`() {
        repo.onPosted(entry("a", Instant.parse("2024-01-01T00:00:00Z")))
        repo.onRemoved("zzz")
        assertEquals(1, repo.notifications.value.size)
    }

    @Test
    fun `list is capped to the most recent entries`() {
        repeat(60) { i -> repo.onPosted(entry("n$i", Instant.ofEpochSecond(1_700_000_000L + i))) }

        val list = repo.notifications.value
        assertEquals(50, list.size)
        assertTrue(list.first().id == "n59")
        assertTrue(list.last().id == "n10")
    }

    private fun entry(id: String, postedAt: Instant, title: String = "t"): NotificationEntry =
        NotificationEntry(
            id = id,
            packageName = "pkg",
            appLabel = "App",
            iconRef = IconRef("pkg", 0),
            title = title,
            text = "",
            postedAt = postedAt,
            isSensitive = false
        )
}
