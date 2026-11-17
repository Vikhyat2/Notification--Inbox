package com.bookverse.notificationinbox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    /** Newest first. Room re-emits whenever the table changes, which keeps the inbox live. */
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC, received_at DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id = :id")
    fun observeById(id: String): Flow<NotificationEntity?>

    /**
     * IGNORE (not REPLACE) so a duplicate never overwrites an existing row:
     * the user's read state is preserved.
     *
     * @return the new row id, or -1 if a row with this id already existed.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: NotificationEntity): Long

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :id AND is_read = 0")
    suspend fun markAsRead(id: String): Int

    @Query("UPDATE notifications SET is_read = 1 WHERE is_read = 0")
    suspend fun markAllAsRead(): Int

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM notifications")
    suspend fun deleteAll(): Int
}
