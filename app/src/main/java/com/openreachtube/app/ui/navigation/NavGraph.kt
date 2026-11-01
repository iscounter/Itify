package com.openreachtube.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.openreachtube.app.ui.screens.home.HomeScreen
import com.openreachtube.app.ui.screens.library.LibraryScreen
import com.openreachtube.app.ui.screens.notifications.NotificationsScreen
import com.openreachtube.app.ui.screens.player.PlayerScreen
import com.openreachtube.app.ui.screens.search.SearchScreen
import com.openreachtube.app.ui.screens.settings.SettingsScreen
import com.openreachtube.app.ui.screens.shorts.ShortsScreen
import com.openreachtube.app.ui.screens.subscriptions.SubscriptionsScreen
import com.openreachtube.app.ui.theme.ZCream
import com.openreachtube.app.ui.theme.ZRed
import com.openreachtube.app.ui.theme.ZTextMuted

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

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Destination.HOME.route,
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
                )
            }
            composable("player?url={url}") { entry ->
                PlayerScreen(url = entry.arguments?.getString("url").orEmpty())
            }
            composable("settings") { SettingsScreen() }
        }

        // Floating pill bottom bar, ZyvoTube-style: hidden on full-screen routes.
        if (current != null) {
            FloatingPillBar(
                current = current,
                onSelect = { dest ->
                    navController.navigate(dest.route) {
                        popUpTo(Destination.HOME.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            )
        }
    }
}

/** Rounded cream pill with a red disc behind the active tab's icon. */
@Composable
private fun FloatingPillBar(
    current: Destination,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = ZCream,
        shadowElevation = 10.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Destination.entries.forEach { dest ->
                val selected = dest == current
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelect(dest) }
                        .padding(horizontal = 10.dp),
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .background(
                                if (selected) ZRed else Color.Transparent,
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            dest.icon,
                            contentDescription = dest.label,
                            tint = if (selected) Color.White else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    Text(
                        dest.label,
                        fontSize = 11.sp,
                        color = if (selected) ZRed else ZTextMuted,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}
