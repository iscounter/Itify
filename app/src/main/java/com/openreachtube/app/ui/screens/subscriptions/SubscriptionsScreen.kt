package com.openreachtube.app.ui.screens.subscriptions

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SubscriptionsScreen(viewModel: SubscriptionsFeedViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LazyColumn(Modifier.fillMaxSize()) {
        if (state.empty) {
            item {
                Text(
                    "No subscriptions yet. Subscribe to your favorite channels to see them here.",
                    Modifier.padding(16.dp),
                )
            }
        }
        items(state.items.size) { index ->
            val video = state.items[index]
            Text(video.title, Modifier.fillMaxSize().padding(16.dp))
        }
    }
}
