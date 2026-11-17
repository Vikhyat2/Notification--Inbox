package com.bookverse.notificationinbox.fcm

/**
 * A validated, normalised notification payload.
 *
 * Produced by [NotificationPayloadParser] from raw FCM data so the rest of the
 * app never has to deal with blank strings, bad URLs or unparseable timestamps.
 */
data class NotificationPayload(
    val id: String,
    val title: String?,
    val description: String?,
    val bannerUrl: String?,
    /** Epoch milliseconds. */
    val timestamp: Long,
)
