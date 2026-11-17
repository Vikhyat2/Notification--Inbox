package com.bookverse.notificationinbox.di


import android.content.Context
import androidx.room.Room
import com.bookverse.notificationinbox.data.NotificationRepository
import com.bookverse.notificationinbox.data.local.AppDatabase

class AppContainer(context: Context) {

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "notification_inbox.db"
        ).build()
    }

    val repository: NotificationRepository by lazy {
        NotificationRepository(database.notificationDao())
    }
}