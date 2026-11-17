package com.bookverse.notificationinbox

import android.app.Application
import com.bookverse.notificationinbox.di.AppContainer
import com.bookverse.notificationinbox.notification.NotificationHelper

class NotificationInboxApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)

    }
}