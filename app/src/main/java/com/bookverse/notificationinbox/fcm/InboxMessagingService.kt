package com.bookverse.notificationinbox.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
//import com.bookverse.notificationinbox.BuildConfig
import com.bookverse.notificationinbox.data.repository.NotificationRepository
import com.google.firebase.messaging.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Receives FCM messages.
 *
 * Called for every data-only message (foreground or background) and for
 * notification + data messages while the app is in the foreground. When the app
 * is in the background and the message has a `notification` block, FCM shows it
 * itself and this method is NOT called; MainActivity imports those when tapped.
 */
@AndroidEntryPoint
class InboxMessagingService : FirebaseMessagingService() {

    @Inject lateinit var repository: NotificationRepository
    @Inject lateinit var systemNotifier: SystemNotifier

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = NotificationPayloadParser.parse(
            data = message.data,
            fallbackTitle = message.notification?.title,
            fallbackBody = message.notification?.body,
            fallbackId = message.messageId,
            fallbackTimestamp = message.sentTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
        )

        if (payload == null) {
            Log.w(TAG, "Ignoring message ${message.messageId}: nothing to display")
            return
        }

        // onMessageReceived already runs on a background thread, and Android may
        // stop the process soon after it returns. Blocking here guarantees the
        // insert finishes before then (it takes a few milliseconds).
        val isNew = runBlocking {
            runCatching { repository.save(payload) }
                .onFailure { Log.e(TAG, "Failed to store '${payload.id}'", it) }
                // If storage fails, still alert the user rather than dropping the message.
                .getOrDefault(true)
        }

        if (isNew) {
            systemNotifier.show(payload)
        } else {
            // Same id already in the inbox: don't add a row and don't alert again.
            Log.d(TAG, "Duplicate notification '${payload.id}' ignored")
        }
    }

    override fun onNewToken(token: String) {
        // No backend in this assignment; the token is logged (debug only) and can be
        // copied from the inbox menu to send test messages.
        if (BuildConfig.DEBUG) Log.d(TAG, "FCM registration token: $token")
    }

    private companion object {
        const val TAG = "InboxMessagingService"
    }
}
