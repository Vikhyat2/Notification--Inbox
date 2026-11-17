package com.bookverse.notificationinbox.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bookverse.notificationinbox.fcm.NotificationPayload
import com.bookverse.notificationinbox.model.InboxNotification

/**
 * Room row for a received notification.
 *
 * The payload `id` is the primary key, which is what makes duplicate handling
 * a database guarantee rather than an in-memory check: a second insert with the
 * same id is ignored (see [NotificationDao.insertIfAbsent]).
 */
@Entity(
    tableName = "notifications",
    indices = [Index(value = ["timestamp"])],
)
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val title: String?,
    val description: String?,
    @ColumnInfo(name = "banner_url")
    val bannerUrl: String?,
    /** Timestamp from the payload (epoch ms); used for sorting and display. */
    val timestamp: Long,
    /** When this device stored the notification (epoch ms); tie-breaker for sorting. */
    @ColumnInfo(name = "received_at")
    val receivedAt: Long,
    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,
)

fun NotificationEntity.toModel() = InboxNotification(
    id = id,
    title = title,
    description = description,
    bannerUrl = bannerUrl,
    timestamp = timestamp,
    isRead = isRead,
)

fun NotificationPayload.toEntity(receivedAt: Long) = NotificationEntity(
    id = id,
    title = title,
    description = description,
    bannerUrl = bannerUrl,
    timestamp = timestamp,
    receivedAt = receivedAt,
    isRead = false,
)
