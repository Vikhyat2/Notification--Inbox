package com.bookverse.notificationinbox.fcm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPayloadParserTest {

    private val now = 1_800_000_000_000L

    private val sample = mapOf(
        "id" to "notification_101",
        "title" to "New Notification",
        "description" to "This is a sample notification for the assignment.",
        "bannerImg" to "https://example.com/banner.jpg",
        "timestamp" to "1791873000000",
    )

    @Test
    fun `parses the sample payload from the spec`() {
        val payload = NotificationPayloadParser.parse(sample, fallbackTimestamp = now)

        assertEquals(
            NotificationPayload(
                id = "notification_101",
                title = "New Notification",
                description = "This is a sample notification for the assignment.",
                bannerUrl = "https://example.com/banner.jpg",
                timestamp = 1_791_873_000_000L,
            ),
            payload,
        )
    }

    @Test
    fun `trims whitespace from values`() {
        val payload = NotificationPayloadParser.parse(
            sample + mapOf("id" to "  notification_101  ", "title" to "  Hello "),
            fallbackTimestamp = now,
        )!!

        assertEquals("notification_101", payload.id)
        assertEquals("Hello", payload.title)
    }

    @Test
    fun `missing banner is null, not an error`() {
        val payload = NotificationPayloadParser.parse(sample - "bannerImg", fallbackTimestamp = now)!!
        assertNull(payload.bannerUrl)
    }

    @Test
    fun `invalid banner urls are dropped`() {
        listOf("", "   ", "null", "not a url", "ftp://example.com/a.jpg", "https://", "/relative/path.jpg")
            .forEach { bad ->
                val payload = NotificationPayloadParser.parse(sample + ("bannerImg" to bad), fallbackTimestamp = now)!!
                assertNull("Expected '$bad' to be rejected", payload.bannerUrl)
            }
    }

    @Test
    fun `http banner urls are accepted`() {
        val payload = NotificationPayloadParser.parse(
            sample + ("bannerImg" to "http://example.com/banner.png"),
            fallbackTimestamp = now,
        )!!
        assertEquals("http://example.com/banner.png", payload.bannerUrl)
    }

    @Test
    fun `spaces in banner urls are encoded`() {
        val payload = NotificationPayloadParser.parse(
            sample + ("bannerImg" to "https://example.com/my banner.jpg"),
            fallbackTimestamp = now,
        )!!
        assertEquals("https://example.com/my%20banner.jpg", payload.bannerUrl)
    }

    @Test
    fun `missing id falls back to the FCM message id`() {
        val payload = NotificationPayloadParser.parse(
            sample - "id",
            fallbackId = "0:1700000000000%abc",
            fallbackTimestamp = now,
        )!!
        assertEquals("0:1700000000000%abc", payload.id)
    }

    @Test
    fun `missing id and message id generates a unique id`() {
        val first = NotificationPayloadParser.parse(sample - "id", fallbackTimestamp = now)!!
        val second = NotificationPayloadParser.parse(sample - "id", fallbackTimestamp = now)!!

        assertTrue(first.id.startsWith("generated_"))
        assertTrue(first.id != second.id)
    }

    @Test
    fun `title and description fall back to the notification block`() {
        val payload = NotificationPayloadParser.parse(
            mapOf("id" to "n1"),
            fallbackTitle = "From notification",
            fallbackBody = "Body from notification",
            fallbackTimestamp = now,
        )!!

        assertEquals("From notification", payload.title)
        assertEquals("Body from notification", payload.description)
    }

    @Test
    fun `data values take priority over the notification block`() {
        val payload = NotificationPayloadParser.parse(
            sample,
            fallbackTitle = "Other title",
            fallbackBody = "Other body",
            fallbackTimestamp = now,
        )!!

        assertEquals("New Notification", payload.title)
    }

    @Test
    fun `title only is enough to be displayable`() {
        val payload = NotificationPayloadParser.parse(mapOf("id" to "n1", "title" to "Hi"), fallbackTimestamp = now)
        assertNotNull(payload)
        assertNull(payload!!.description)
    }

    @Test
    fun `payload with nothing to display is rejected`() {
        assertNull(NotificationPayloadParser.parse(emptyMap(), fallbackTimestamp = now))
        assertNull(
            NotificationPayloadParser.parse(
                mapOf("id" to "n1", "title" to " ", "description" to "", "bannerImg" to "bad"),
                fallbackTimestamp = now,
            ),
        )
    }

    @Test
    fun `timestamp in milliseconds is kept`() {
        assertEquals(1_791_873_000_000L, NotificationPayloadParser.parseTimestamp("1791873000000", now))
    }

    @Test
    fun `timestamp in seconds is converted to milliseconds`() {
        assertEquals(1_791_873_000_000L, NotificationPayloadParser.parseTimestamp("1791873000", now))
    }

    @Test
    fun `decimal timestamp is accepted`() {
        assertEquals(1_791_873_000_000L, NotificationPayloadParser.parseTimestamp("1791873000000.0", now))
    }

    @Test
    fun `missing or invalid timestamps use the fallback`() {
        listOf(null, "", "  ", "abc", "0", "-5", "NaN", "Infinity").forEach { bad ->
            assertEquals("Expected fallback for '$bad'", now, NotificationPayloadParser.parseTimestamp(bad, now))
        }
    }
}
