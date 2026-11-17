package com.bookverse.notificationinbox

import android.app.Application
import com.bookverse.notificationinbox.di.AppContainer

class NotificationInboxApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}