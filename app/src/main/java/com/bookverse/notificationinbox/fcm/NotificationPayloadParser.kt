package com.bookverse.notificationinbox.fcm

import java.net.URI
import java.util.UUID

/**
 * Converts raw FCM key/value data into a [NotificationPayload].
 *
 * Pure Kotlin with no Android dependencies, so every edge case is covered by
 * plain JVM unit tests (see NotificationPayloadParserTest).
 *
 * Expected payload:
 * ```
 * {
 *   "id": "notification_101",
 *   "title": "New Notification",
 *   "description": "This is a sample notification for the assignment.",
 *   "bannerImg": "https://example.com/banner.jpg",
 *   "timestamp": "1791873000000"
 * }
 * ```
 */
object NotificationPayloadParser {

    const val KEY_ID = "id"
    const val KEY_TITLE = "title"
    const val KEY_DESCRIPTION = "description"
    const val KEY_BANNER = "bannerImg"
    const val KEY_TIMESTAMP = "timestamp"

    val PAYLOAD_KEYS = listOf(KEY_ID, KEY_TITLE, KEY_DESCRIPTION, KEY_BANNER, KEY_TIMESTAMP)

    /**
     * Values below this are treated as epoch *seconds* rather than milliseconds.
     * 100_000_000_000 ms is March 1973, while 100_000_000_000 s is far in the
     * future, so the two ranges never overlap for realistic data.
     */
    private const val SECONDS_THRESHOLD = 100_000_000_000L

    /**
     * @param data FCM data payload.
     * @param fallbackTitle title from an FCM `notification` block, if one was sent.
     * @param fallbackBody body from an FCM `notification` block, if one was sent.
     * @param fallbackId used when the payload has no `id` (normally the FCM message id,
     *        which stays stable across redeliveries of the same message).
     * @param fallbackTimestamp used when `timestamp` is missing or invalid.
     * @return the parsed payload, or `null` when there is nothing displayable
     *         (no title, no description and no banner).
     */
    fun parse(
        data: Map<String, String?>,
        fallbackTitle: String? = null,
        fallbackBody: String? = null,
        fallbackId: String? = null,
        fallbackTimestamp: Long,
    ): NotificationPayload? {
        val title = data[KEY_TITLE].cleaned() ?: fallbackTitle.cleaned()
        val description = data[KEY_DESCRIPTION].cleaned() ?: fallbackBody.cleaned()
        val bannerUrl = normalizeImageUrl(data[KEY_BANNER])

        if (title == null && description == null && bannerUrl == null) return null

        val id = data[KEY_ID].cleaned()
            ?: fallbackId.cleaned()
            ?: "generated_${UUID.randomUUID()}"

        return NotificationPayload(
            id = id,
            title = title,
            description = description,
            bannerUrl = bannerUrl,
            timestamp = parseTimestamp(data[KEY_TIMESTAMP], fallbackTimestamp),
        )
    }

    /**
     * Accepts epoch milliseconds or seconds, as a string ("1791873000000") or
     * decimal ("1791873000.0"). Falls back for anything missing, malformed,
     * zero or negative.
     */
    fun parseTimestamp(raw: String?, fallback: Long): Long {
        val trimmed = raw?.trim().orEmpty()
        val value = trimmed.toLongOrNull()
            ?: trimmed.toDoubleOrNull()?.takeIf { it.isFinite() }?.toLong()
            ?: return fallback
        if (value <= 0L) return fallback
        return if (value < SECONDS_THRESHOLD) value * 1000 else value
    }

    /**
     * Returns the URL only if it is an absolute http(s) URL with a host;
     * anything else is treated as "no banner" so the UI shows a placeholder.
     */
    fun normalizeImageUrl(raw: String?): String? {
        // Unencoded spaces are common in hand-written payloads and would otherwise
        // make an otherwise valid URL unparseable.
        val candidate = raw.cleaned()?.replace(" ", "%20") ?: return null
        val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        return candidate
    }

    /** Trims, and treats blank values and the literal string "null" as missing. */
    private fun String?.cleaned(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
}
