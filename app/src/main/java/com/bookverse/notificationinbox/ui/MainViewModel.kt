package com.bookverse.notificationinbox.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookverse.notificationinbox.fcm.NotificationPayloadParser
import com.bookverse.notificationinbox.data.repository.NotificationRepository
//import com.bookverse.notificationinbox.fcm.NotificationPayloadParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    /**
     * Stores a notification that FCM displayed itself (app was in the background
     * and the message had a `notification` block) and that the user then tapped.
     * In that case onMessageReceived never ran, so this is the only chance to
     * add it to the inbox. Duplicates are ignored by the database as usual.
     */
    fun importTappedNotification(data: Map<String, String>, messageId: String) {
        val payload = NotificationPayloadParser.parse(
            data = data,
            fallbackId = messageId,
            fallbackTimestamp = System.currentTimeMillis(),
        ) ?: return

        viewModelScope.launch {
            try {
                repository.save(payload)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import tapped notification '${payload.id}'", e)
            }
        }
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
