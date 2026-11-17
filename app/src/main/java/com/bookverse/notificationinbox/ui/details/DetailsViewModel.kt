package com.bookverse.notificationinbox.ui.details

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookverse.notificationinbox.data.repository.NotificationRepository
import com.bookverse.notificationinbox.fcm.SystemNotifier
import com.bookverse.notificationinbox.model.InboxNotification
import com.bookverse.notificationinbox.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Loaded(val notification: InboxNotification) : DetailsUiState
    /** The id doesn't exist (e.g. it was deleted or the inbox was cleared). */
    data object NotFound : DetailsUiState
}

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NotificationRepository,
    private val systemNotifier: SystemNotifier,
) : ViewModel() {

    private val notificationId: String = savedStateHandle.get<String>(Routes.ARG_ID).orEmpty()

    val uiState: StateFlow<DetailsUiState> = repository.observeNotification(notificationId)
        .map { notification ->
            if (notification == null) DetailsUiState.NotFound else DetailsUiState.Loaded(notification)
        }
        .catch { error ->
            Log.e(TAG, "Failed to load '$notificationId'", error)
            emit(DetailsUiState.NotFound)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailsUiState.Loading,
        )

    private val _deleted = MutableStateFlow(false)

    /** Becomes true once the notification has been deleted; the screen then navigates back. */
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    init {
        // Runs once per visit to the screen (ViewModels survive rotation).
        // Room re-emits the row, so the inbox updates to "read" automatically.
        viewModelScope.launch {
            runSafely { repository.markAsRead(notificationId) }
            // The user has now seen it, so its system-tray entry is no longer needed.
            systemNotifier.cancel(notificationId)
        }
    }

    fun delete() {
        if (_deleted.value) return
        viewModelScope.launch {
            runSafely { repository.delete(notificationId) }
            _deleted.value = true
        }
    }

    private suspend fun runSafely(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Action failed for '$notificationId'", e)
        }
    }

    private companion object {
        const val TAG = "DetailsViewModel"
    }
}
