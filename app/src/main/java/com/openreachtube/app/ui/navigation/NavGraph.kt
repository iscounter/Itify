package com.openreachtube.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.openreachtube.app.ui.screens.channel.ChannelScreen
import com.openreachtube.app.ui.screens.downloads.DownloadsScreen
import com.openreachtube.app.ui.screens.home.HomeScreen
import com.openreachtube.app.ui.screens.library.LibraryScreen
import com.openreachtube.app.ui.screens.notifications.NotificationsScreen
import com.openreachtube.app.ui.screens.player.PlayerScreen
import com.openreachtube.app.ui.screens.search.SearchScreen
import com.openreachtube.app.ui.screens.settings.SettingsScreen
import com.openreachtube.app.ui.screens.shorts.ShortsScreen
import com.openreachtube.app.ui.screens.subscriptions.SubscriptionsScreen

private val Destination.icon: ImageVector
    get() = when (this) {
        Destination.HOME -> Icons.Filled.Home
        Destination.SHORTS -> Icons.Filled.Whatshot
        Destination.SUBSCRIPTIONS -> Icons.Filled.Subscriptions
        Destination.LIBRARY -> Icons.Filled.Person
        Destination.NOTIFICATIONS -> Icons.Filled.Notifications
    }

@Composable
fun OpenReachNavGraph(deepLink: String? = null) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = Destination.fromRoute(backStack?.destination?.route)

    LaunchedEffect(deepLink) {
        if (deepLink != null) {
            navController.navigate("player?url=${android.net.Uri.encode(deepLink)}")
        }
    }

    Scaffold(
        bottomBar = {
            if (current != null) {
                NavigationBar {
                    Destination.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = current == dest,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(Destination.HOME.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.HOME.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.HOME.route) {
                HomeScreen(
                    onOpenVideo = { url ->
                        navController.navigate("player?url=${android.net.Uri.encode(url)}")
                    },
                    onOpenSearch = { navController.navigate("search") },
                    onOpenSettings = { navController.navigate("settings") },
                )
            }
            composable(Destination.SHORTS.route) { ShortsScreen() }
            composable(Destination.SUBSCRIPTIONS.route) {
                SubscriptionsScreen(
                    onOpenVideo = { url ->
                        navController.navigate("player?url=${android.net.Uri.encode(url)}")
                    },
                )
            }
            composable(Destination.LIBRARY.route) {
                LibraryScreen(
                    onOpenVideo = { url ->
                        navController.navigate("player?url=${android.net.Uri.encode(url)}")
                    },
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenDownloads = { navController.navigate("downloads") },
                )
            }
            composable(Destination.NOTIFICATIONS.route) {
                NotificationsScreen(
                    onOpenVideo = { url ->
                        navController.navigate("player?url=${android.net.Uri.encode(url)}")
                    },
                )
            }
            composable("search?query={query}") { entry ->
                SearchScreen(
                    initialQuery = entry.arguments?.getString("query").orEmpty(),
                    onOpenVideo = { url ->
                        navController.navigate("player?url=${android.net.Uri.encode(url)}")
                    },
                    onOpenChannel = { channelUrl ->
                        navController.navigate("channel?url=${android.net.Uri.encode(channelUrl)}")
                    },
                )
            }
            composable("player?url={url}") { entry ->
                PlayerScreen(
                    url = entry.arguments?.getString("url").orEmpty(),
                    onOpenChannel = { channelUrl ->
                        navController.navigate("channel?url=${android.net.Uri.encode(channelUrl)}")
                    },
                )
            }
            composable("channel?url={url}") { entry ->
                ChannelScreen(
                    url = entry.arguments?.getString("url").orEmpty(),
                    onOpenVideo = { url ->
                        navController.navigate("player?url=${android.net.Uri.encode(url)}")
                    },
                )
            }
            composable("downloads") {
                DownloadsScreen(onBack = { navController.popBackStack() })
            }
            composable("settings") { SettingsScreen() }
        }
    }
}
