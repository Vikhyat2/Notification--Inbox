package com.bookverse.notificationinbox.di

import android.content.Context
import androidx.room.Room
import com.bookverse.notificationinbox.data.local.AppDatabase
import com.bookverse.notificationinbox.data.local.NotificationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun provideNotificationDao(database: AppDatabase): NotificationDao =
        database.notificationDao()
}
