package com.bookverse.notificationinbox.ui.inbox

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.bookverse.notificationinbox.data.repository.NotificationRepository
import com.bookverse.notificationinbox.fcm.SystemNotifier
import com.bookverse.notificationinbox.model.InboxNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

enum class InboxFilter { ALL, UNREAD }

data class InboxUiState(
    val notifications: List<InboxNotification> = emptyList(),
    val unreadCount: Int = 0,
    val totalCount: Int = 0,
    val filter: InboxFilter = InboxFilter.ALL,
    val isLoading: Boolean = true,
)

/** One-off events the screen reacts to once (snackbars, clipboard). */
sealed interface InboxEvent {
    data class TokenFetched(val token: String) : InboxEvent
    data object TokenUnavailable : InboxEvent
}

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val systemNotifier: SystemNotifier,
) : ViewModel() {

    private val filter = MutableStateFlow(InboxFilter.ALL)

    val uiState: StateFlow<InboxUiState> =
        combine(repository.observeNotifications(), filter) { all, selectedFilter ->
            InboxUiState(
                notifications = when (selectedFilter) {
                    InboxFilter.ALL -> all
                    InboxFilter.UNREAD -> all.filterNot { it.isRead }
                },
                unreadCount = all.count { !it.isRead },
                totalCount = all.size,
                filter = selectedFilter,
                isLoading = false,
            )
        }
            .catch { error ->
                // A database failure should show an empty inbox, not crash the app.
                Log.e(TAG, "Failed to load notifications", error)
                emit(InboxUiState(isLoading = false))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = InboxUiState(),
            )

    private val _events = Channel<InboxEvent>(Channel.BUFFERED)
    val events: Flow<InboxEvent> = _events.receiveAsFlow()

    fun setFilter(newFilter: InboxFilter) {
        filter.value = newFilter
    }

    fun markAllAsRead() = launchSafely { repository.markAllAsRead() }

    fun clearInbox() = launchSafely {
        repository.clearAll()
        systemNotifier.cancelAll()
    }

    /** Fetches the FCM token so it can be copied and used to send test messages. */
    fun requestFcmToken() {
        viewModelScope.launch {
            val event = try {
                InboxEvent.TokenFetched(FirebaseMessaging.getInstance().token.await())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch FCM token", e)
                InboxEvent.TokenUnavailable
            }
            _events.send(event)
        }
    }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Inbox action failed", e)
            }
        }
    }

    private companion object {
        const val TAG = "InboxViewModel"
    }
}
