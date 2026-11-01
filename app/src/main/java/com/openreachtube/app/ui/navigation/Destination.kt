package com.openreachtube.app.ui.navigation

/**
 * Five-tab shell mirroring the measured information architecture. fromRoute
 * returns null for full-screen routes (player, search, settings) so the pill
 * bar can hide itself.
 */
enum class Destination(val route: String, val label: String) {
    HOME("home", "Home"),
    SHORTS("shorts", "Shorts"),
    SUBSCRIPTIONS("subscriptions", "Subscriptions"),
    LIBRARY("library", "You"),
    NOTIFICATIONS("notifications", "Notifications");

    companion object {
        fun fromRoute(route: String?): Destination? =
            entries.firstOrNull { it.route == route }
    }
}
