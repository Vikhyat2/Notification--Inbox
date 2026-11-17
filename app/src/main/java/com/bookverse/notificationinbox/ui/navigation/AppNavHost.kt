package com.bookverse.notificationinbox.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bookverse.notificationinbox.ui.details.DetailsScreen
import com.bookverse.notificationinbox.ui.inbox.InboxScreen
import kotlinx.coroutines.flow.Flow

object Routes {
    const val INBOX = "inbox"
    const val ARG_ID = "id"
    const val DETAILS = "details/{$ARG_ID}"

    /** Ids come from the server, so encode them to keep the route valid. */
    fun details(id: String) = "details/${Uri.encode(id)}"
}

/**
 * Inbox → Details.
 *
 * [openInboxRequests] fires when a system notification is tapped while the app
 * is already open, so the user always lands on the inbox even if a details
 * screen was showing.
 */
@Composable
fun AppNavHost(
    openInboxRequests: Flow<Unit>,
    navController: NavHostController = rememberNavController(),
) {
    LaunchedEffect(navController, openInboxRequests) {
        openInboxRequests.collect {
            navController.popBackStack(Routes.INBOX, inclusive = false)
        }
    }

    NavHost(navController = navController, startDestination = Routes.INBOX) {
        composable(Routes.INBOX) {
            InboxScreen(
                onNotificationClick = { id ->
                    navController.navigate(Routes.details(id)) { launchSingleTop = true }
                },
            )
        }
        composable(
            route = Routes.DETAILS,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
        ) {
            // navigateUp is a no-op on the start destination, so a double tap on
            // Back can never pop the inbox and leave a blank screen.
            DetailsScreen(onBack = { navController.navigateUp() })
        }
    }
}
