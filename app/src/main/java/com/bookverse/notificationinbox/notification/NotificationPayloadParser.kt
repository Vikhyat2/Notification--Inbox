package com.bookverse.notificationinbox.notification

import com.bookverse.notificationinbox.model.AppNotification

object NotificationPayloadParser {

    private const val DEFAULT_TITLE = "New notification"

    /**
     * Converts an FCM data map into an [AppNotification].
     * Returns null only if no usable id exists (we can't de-duplicate without one).
     */
    fun parse(
        data: Map<String, String>,
        fallbackId: String? = null,
        fallbackTitle: String? = null,
        fallbackBody: String? = null,
        fallbackImage: String? = null
    ): AppNotification? {
        val id = data["id"].clean() ?: fallbackId.clean() ?: return null

        return AppNotification(
            id = id,
            title = data["title"].clean() ?: fallbackTitle.clean() ?: DEFAULT_TITLE,
            description = data["description"].clean() ?: fallbackBody.clean() ?: "",
            bannerImg = parseBanner(data["bannerImg"] ?: fallbackImage),
            timestamp = parseTimestamp(data["timestamp"]),
            isRead = false
        )
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    /** Keep only http/https URLs; anything else is treated as "no banner". */
    private fun parseBanner(raw: String?): String? =
        raw.clean()?.takeIf {
            it.startsWith("http://", ignoreCase = true) ||
                    it.startsWith("https://", ignoreCase = true)
        }

    /** Accepts epoch millis (or seconds); falls back to "now" if missing/invalid. */
    private fun parseTimestamp(raw: String?): Long {
        val value = raw.clean()?.toLongOrNull()?.takeIf { it > 0 }
            ?: return System.currentTimeMillis()
        return if (value < 100_000_000_000L) value * 1000 else value  // seconds → millis
    }
}