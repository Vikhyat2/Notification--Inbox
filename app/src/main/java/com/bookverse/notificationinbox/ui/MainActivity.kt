package com.bookverse.notificationinbox.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.bookverse.notificationinbox.fcm.NotificationPayloadParser
import com.bookverse.notificationinbox.ui.navigation.AppNavHost
import com.bookverse.notificationinbox.ui.theme.NotificationInboxTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** Emits when a notification tap arrives while the activity is already running. */
    private val openInboxRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Only on a fresh launch; after rotation the same intent would be re-read.
        if (savedInstanceState == null) importFromNotificationTap(intent)

        // The inbox is the start destination, so a cold start from a
        // notification tap already lands on it.
        setContent {
            NotificationInboxTheme {
                AppNavHost(openInboxRequests = openInboxRequests)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        importFromNotificationTap(intent)
        if (intent.action == ACTION_OPEN_INBOX || intent.hasExtra(FCM_MESSAGE_ID_KEY)) {
            openInboxRequests.tryEmit(Unit)
        }
    }

    private fun importFromNotificationTap(intent: Intent?) {
        val extras = intent?.extras ?: return
        // Present only when the launch came from a notification FCM displayed itself.
        val messageId = extras.getString(FCM_MESSAGE_ID_KEY) ?: return
        val data = NotificationPayloadParser.PAYLOAD_KEYS
            .mapNotNull { key -> extras.getString(key)?.let { key to it } }
            .toMap()
        viewModel.importTappedNotification(data, messageId)
    }

    companion object {
        const val ACTION_OPEN_INBOX = "com.prerna.notificationinbox.action.OPEN_INBOX"
        private const val FCM_MESSAGE_ID_KEY = "google.message_id"

        /** Intent used by our own system notifications: System Notification → Inbox. */
        fun openInboxIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_OPEN_INBOX)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
    }
}
