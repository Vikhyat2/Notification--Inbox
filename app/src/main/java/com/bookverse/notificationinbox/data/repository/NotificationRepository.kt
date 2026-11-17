package com.bookverse.notificationinbox.data.repository

import com.bookverse.notificationinbox.data.local.NotificationDao
import com.bookverse.notificationinbox.data.local.toEntity
import com.bookverse.notificationinbox.data.local.toModel
import com.bookverse.notificationinbox.fcm.NotificationPayload
import com.bookverse.notificationinbox.model.InboxNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for notifications. Screens and the messaging service
 * talk to this class, never to Room directly.
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val dao: NotificationDao,
) {

    fun observeNotifications(): Flow<List<InboxNotification>> =
        dao.observeAll().map { rows -> rows.map { it.toModel() } }

    fun observeNotification(id: String): Flow<InboxNotification?> =
        dao.observeById(id).map { it?.toModel() }

    /**
     * Stores the payload unless one with the same id already exists.
     *
     * @return `true` if this is a new notification, `false` if it was a duplicate.
     */
    suspend fun save(
        payload: NotificationPayload,
        receivedAt: Long = System.currentTimeMillis(),
    ): Boolean = dao.insertIfAbsent(payload.toEntity(receivedAt)) != -1L

    suspend fun markAsRead(id: String) {
        dao.markAsRead(id)
    }

    suspend fun markAllAsRead() {
        dao.markAllAsRead()
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }
}
