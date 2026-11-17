package com.bookverse.notificationinbox.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.notificationinbox.R
import com.bookverse.notificationinbox.model.InboxNotification
import com.bookverse.notificationinbox.ui.components.BannerImage
import com.bookverse.notificationinbox.ui.components.EmptyState
import com.bookverse.notificationinbox.ui.theme.NotificationInboxTheme
import com.bookverse.notificationinbox.ui.util.formatRelativeTime
import com.bookverse.notificationinbox.ui.util.rememberCurrentTimeMillis

@Composable
fun InboxScreen(
    onNotificationClick: (String) -> Unit,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is InboxEvent.TokenFetched -> {
                    clipboard.setText(AnnotatedString(event.token))
                    snackbarHostState.showSnackbar(context.getString(R.string.token_copied))
                }
                InboxEvent.TokenUnavailable ->
                    snackbarHostState.showSnackbar(context.getString(R.string.token_unavailable))
            }
        }
    }

    InboxContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNotificationClick = onNotificationClick,
        onFilterChange = viewModel::setFilter,
        onMarkAllRead = viewModel::markAllAsRead,
        onClearInbox = viewModel::clearInbox,
        onCopyToken = viewModel::requestFcmToken,
        permissionBanner = {
            NotificationPermissionBanner(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        },
    )
}

/** Stateless so it can be previewed and reasoned about in isolation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InboxContent(
    uiState: InboxUiState,
    snackbarHostState: SnackbarHostState,
    onNotificationClick: (String) -> Unit,
    onFilterChange: (InboxFilter) -> Unit,
    onMarkAllRead: () -> Unit,
    onClearInbox: () -> Unit,
    onCopyToken: () -> Unit,
    permissionBanner: @Composable () -> Unit,
) {
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.inbox_title))
                        if (uiState.unreadCount > 0) {
                            Text(
                                text = stringResource(R.string.unread_count, uiState.unreadCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                actions = {
                    InboxMenu(
                        canMarkAllRead = uiState.unreadCount > 0,
                        canClear = uiState.totalCount > 0,
                        onMarkAllRead = onMarkAllRead,
                        onClearInbox = { showClearDialog = true },
                        onCopyToken = onCopyToken,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            permissionBanner()

            FilterRow(
                selected = uiState.filter,
                unreadCount = uiState.unreadCount,
                onFilterChange = onFilterChange,
            )

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                uiState.notifications.isEmpty() -> InboxEmptyState(uiState.filter)

                else -> NotificationList(
                    notifications = uiState.notifications,
                    onNotificationClick = onNotificationClick,
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_dialog_title)) },
            text = { Text(stringResource(R.string.clear_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClearInbox()
                }) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun InboxMenu(
    canMarkAllRead: Boolean,
    canClear: Boolean,
    onMarkAllRead: () -> Unit,
    onClearInbox: () -> Unit,
    onCopyToken: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.menu_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_mark_all_read)) },
                leadingIcon = { Icon(Icons.Outlined.DoneAll, contentDescription = null) },
                enabled = canMarkAllRead,
                onClick = {
                    expanded = false
                    onMarkAllRead()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_clear_inbox)) },
                leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
                enabled = canClear,
                onClick = {
                    expanded = false
                    onClearInbox()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_copy_token)) },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCopyToken()
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    selected: InboxFilter,
    unreadCount: Int,
    onFilterChange: (InboxFilter) -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == InboxFilter.ALL,
            onClick = { onFilterChange(InboxFilter.ALL) },
            label = { Text(stringResource(R.string.filter_all)) },
        )
        FilterChip(
            selected = selected == InboxFilter.UNREAD,
            onClick = { onFilterChange(InboxFilter.UNREAD) },
            label = {
                Text(
                    if (unreadCount > 0) {
                        stringResource(R.string.filter_unread_count, unreadCount)
                    } else {
                        stringResource(R.string.filter_unread)
                    },
                )
            },
        )
    }
}

@Composable
private fun InboxEmptyState(filter: InboxFilter) {
    when (filter) {
        InboxFilter.ALL -> EmptyState(
            icon = Icons.Outlined.NotificationsNone,
            title = stringResource(R.string.empty_inbox_title),
            body = stringResource(R.string.empty_inbox_body),
        )
        InboxFilter.UNREAD -> EmptyState(
            icon = Icons.Outlined.DoneAll,
            title = stringResource(R.string.empty_unread_title),
            body = stringResource(R.string.empty_unread_body),
        )
    }
}

@Composable
private fun NotificationList(
    notifications: List<InboxNotification>,
    onNotificationClick: (String) -> Unit,
) {
    val now = rememberCurrentTimeMillis()
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = notifications, key = { it.id }) { notification ->
            InboxItem(
                notification = notification,
                now = now,
                onClick = { onNotificationClick(notification.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InboxItem(
    notification: InboxNotification,
    now: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unread = !notification.isRead
    val unreadLabel = stringResource(R.string.unread_indicator)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (unread) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            BannerImage(
                url = notification.bannerUrl,
                contentDescription = null, // decorative here; the title describes the item
                placeholderIconSize = 24.dp,
                modifier = Modifier
                    .size(width = 96.dp, height = 72.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title ?: stringResource(R.string.untitled_notification),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (unread) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .semantics { contentDescription = unreadLabel },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notification.description ?: stringResource(R.string.no_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatRelativeTime(
                        timestamp = notification.timestamp,
                        now = now,
                        justNow = stringResource(R.string.time_just_now),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InboxContentPreview() {
    val now = System.currentTimeMillis()
    NotificationInboxTheme(dynamicColor = false) {
        InboxContent(
            uiState = InboxUiState(
                notifications = listOf(
                    InboxNotification("1", "New Notification", "This is a sample notification for the assignment.", null, now - 120_000, isRead = false),
                    InboxNotification("2", "Weekly digest", "Your summary for this week is ready.", null, now - 86_400_000, isRead = true),
                    InboxNotification("3", null, null, null, now - 3 * 86_400_000L, isRead = true),
                ),
                unreadCount = 1,
                totalCount = 3,
                isLoading = false,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNotificationClick = {},
            onFilterChange = {},
            onMarkAllRead = {},
            onClearInbox = {},
            onCopyToken = {},
            permissionBanner = {},
        )
    }
}
