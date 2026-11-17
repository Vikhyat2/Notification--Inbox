package com.bookverse.notificationinbox.fcm

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bookverse.notificationinbox.R
import com.bookverse.notificationinbox.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything to do with Android *system* notifications: the channel, building
 * and posting a notification, and cancelling it.
 */
@Singleton
class SystemNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    /** Safe to call repeatedly; creating an existing channel is a no-op. */
    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            context.getString(R.string.notification_channel_id),
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    /**
     * Posts a system notification for [payload]. Tapping it opens the inbox.
     *
     * The payload id is used as the notification tag, so if the same id were
     * ever posted again it would replace the existing notification instead of
     * stacking a second one.
     */
    @SuppressLint("MissingPermission") // Checked via areNotificationsEnabled() below.
    fun show(payload: NotificationPayload) {
        if (!notificationManager.areNotificationsEnabled()) {
            Log.i(TAG, "Notifications disabled; '${payload.id}' saved to inbox only")
            return
        }

        val title = payload.title ?: context.getString(R.string.notification_fallback_title)
        val notification = NotificationCompat.Builder(
            context,
            context.getString(R.string.notification_channel_id),
        )
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(payload.description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.description))
            .setWhen(payload.timestamp)
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Android 7.x has no channels
            .setAutoCancel(true)
            .setContentIntent(openInboxPendingIntent(payload.id))
            .build()

        try {
            notificationManager.notify(payload.id, NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call.
            Log.w(TAG, "Could not post notification '${payload.id}'", e)
        }
    }

    fun cancel(id: String) = notificationManager.cancel(id, NOTIFICATION_ID)

    fun cancelAll() = notificationManager.cancelAll()

    private fun openInboxPendingIntent(id: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            id.hashCode(), // distinct request codes keep each PendingIntent separate
            MainActivity.openInboxIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private companion object {
        const val TAG = "SystemNotifier"

        /** Constant id; the payload id is the tag, so (tag, id) is unique per notification. */
        const val NOTIFICATION_ID = 1
    }
}
