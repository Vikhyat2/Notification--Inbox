package com.bookverse.notificationinbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.bookverse.notificationinbox.R

/**
 * Loads a banner image, showing a neutral placeholder while loading and when
 * the URL is missing, invalid or fails to load. A broken image never affects
 * the rest of the notification content.
 */
@Composable
fun BannerImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholderIconSize: Dp = 32.dp,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url == null) {
            PlaceholderIcon(placeholderIconSize)
        } else {
            RemoteImage(url, contentDescription, placeholderIconSize)
        }
    }
}

@Composable
private fun RemoteImage(url: String, contentDescription: String?, placeholderIconSize: Dp) {
    // Keyed on the URL so a recycled list item never shows a stale state.
    var state by remember(url) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        onState = { state = it },
        modifier = Modifier.fillMaxSize(),
    )

    // Covers loading and error (404, bad host, non-image, cleartext blocked, offline).
    if (state !is AsyncImagePainter.State.Success) {
        PlaceholderIcon(placeholderIconSize)
    }
}

@Composable
private fun PlaceholderIcon(size: Dp) {
    Icon(
        painter = painterResource(R.drawable.ic_image_placeholder),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(size),
    )
}
