package com.bookverse.notificationinbox.data.local


import androidx.room.RoomDatabase
import androidx.room3.Database

@Database(entities = [NotificationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
}