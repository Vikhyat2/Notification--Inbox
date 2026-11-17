package com.bookverse.notificationinbox.notification

import android.util.Log
import com.bookverse.notificationinbox.NotificationInboxApp
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking

class InboxMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = NotificationPayloadParser.parse(
            data = message.data,
            fallbackId = message.messageId,
            fallbackTitle = message.notification?.title,
            fallbackBody = message.notification?.body,
            fallbackImage = message.notification?.imageUrl?.toString()
        ) ?: run {
            Log.w(TAG, "Message ignored: no usable id")
            return
        }

        val repository = (application as NotificationInboxApp).container.repository

        // onMessageReceived already runs on a background thread, so blocking here
        // is safe and guarantees the save finishes before the service can be stopped.
        val isNew = runCatching { runBlocking { repository.save(notification) } }
            .onFailure { Log.e(TAG, "Failed to save notification ${notification.id}", it) }
            .getOrDefault(false)

        if (isNew) {
            NotificationHelper.show(this, notification)
        } else {
            Log.d(TAG, "Duplicate or unsaved id=${notification.id}, not shown again")
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token: $token")
    }

    companion object {
        private const val TAG = "FCM"
    }
}