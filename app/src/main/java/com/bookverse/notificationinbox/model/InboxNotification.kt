package com.bookverse.notificationinbox.model

/**
 * A notification as the UI sees it. Decoupled from the Room entity so the
 * persistence layer can change without touching screens.
 *
 * [title], [description] and [bannerUrl] are nullable because the payload may
 * omit them; the UI decides how to present missing values.
 */
data class InboxNotification(
    val id: String,
    val title: String?,
    val description: String?,
    val bannerUrl: String?,
    val timestamp: Long,
    val isRead: Boolean,
)
