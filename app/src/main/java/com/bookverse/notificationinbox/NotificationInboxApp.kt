package com.bookverse.notificationinbox

import android.app.Application
import com.bookverse.notificationinbox.fcm.SystemNotifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NotificationInboxApp : Application() {

    @Inject lateinit var systemNotifier: SystemNotifier

    override fun onCreate() {
        super.onCreate()
        // Create the channel up front so it exists even for notifications FCM
        // displays itself while the app is in the background.
        systemNotifier.createChannel()
    }
}
