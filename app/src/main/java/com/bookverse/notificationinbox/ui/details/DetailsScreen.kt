package com.bookverse.notificationinbox.ui.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.notificationinbox.R
import com.bookverse.notificationinbox.model.InboxNotification
import com.bookverse.notificationinbox.ui.components.BannerImage
import com.bookverse.notificationinbox.ui.components.EmptyState
import com.bookverse.notificationinbox.ui.theme.NotificationInboxTheme
import com.bookverse.notificationinbox.ui.util.formatFullDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()

    LaunchedEffect(deleted) {
        if (deleted) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (uiState is DetailsUiState.Loaded) {
                        IconButton(onClick = viewModel::delete) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()

        when (val state = uiState) {
            DetailsUiState.Loading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            // Hidden while a delete is completing to avoid a flash before navigating back.
            DetailsUiState.NotFound -> if (!deleted) {
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = stringResource(R.string.details_not_found),
                    body = null,
                    modifier = contentModifier,
                )
            }

            is DetailsUiState.Loaded -> DetailsContent(
                notification = state.notification,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun DetailsContent(
    notification: InboxNotification,
    modifier: Modifier = Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        BannerImage(
            url = notification.bannerUrl,
            contentDescription = stringResource(R.string.banner_image),
            placeholderIconSize = 48.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        )

        Column(Modifier.padding(16.dp)) {
            Text(
                text = notification.title ?: stringResource(R.string.untitled_notification),
                style = MaterialTheme.typography.headlineSmall,
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatFullDateTime(notification.timestamp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))

            SelectionContainer {
                Text(
                    text = notification.description ?: stringResource(R.string.no_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (notification.description == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailsContentPreview() {
    NotificationInboxTheme(dynamicColor = false) {
        DetailsContent(
            InboxNotification(
                id = "notification_101",
                title = "New Notification",
                description = "This is a sample notification for the assignment.",
                bannerUrl = null,
                timestamp = 1_791_873_000_000L,
                isRead = true,
            ),
        )
    }
}
