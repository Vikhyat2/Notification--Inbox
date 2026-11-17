package com.bookverse.notificationinbox.data.local

import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Dao
import androidx.room3.Insert
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    /** Returns the new rowId, or -1 if a notification with this id already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: NotificationEntity): Long

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id = :id")
    fun observeById(id: String): Flow<NotificationEntity?>

    @Query("SELECT COUNT(*) FROM notifications WHERE is_read = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET is_read = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}