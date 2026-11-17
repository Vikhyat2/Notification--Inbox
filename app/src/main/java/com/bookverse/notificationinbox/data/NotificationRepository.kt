package com.bookverse.notificationinbox.data


import com.bookverse.notificationinbox.data.local.NotificationDao
import com.bookverse.notificationinbox.data.local.NotificationEntity
import com.bookverse.notificationinbox.data.local.toModel
import com.bookverse.notificationinbox.model.AppNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationRepository(private val dao: NotificationDao) {

    val notifications: Flow<List<AppNotification>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    val unreadCount: Flow<Int> = dao.observeUnreadCount()

    fun observeNotification(id: String): Flow<AppNotification?> =
        dao.observeById(id).map { it?.toModel() }

    /** @return true if saved, false if it was a duplicate id. */
    suspend fun save(notification: AppNotification): Boolean {
        val entity = NotificationEntity(
            id = notification.id,
            title = notification.title,
            description = notification.description,
            bannerImg = notification.bannerImg,
            timestamp = notification.timestamp,
            isRead = false
        )
        return dao.insert(entity) != -1L
    }

    suspend fun markAsRead(id: String) = dao.markAsRead(id)
    suspend fun markAllAsRead() = dao.markAllAsRead()
    suspend fun delete(id: String) = dao.delete(id)
    suspend fun clearAll() = dao.deleteAll()
}