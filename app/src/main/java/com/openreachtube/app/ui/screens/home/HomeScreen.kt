package com.openreachtube.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openreachtube.app.ui.components.CategoryChipRow
import com.openreachtube.app.ui.components.RailCard
import com.openreachtube.app.ui.components.SectionTitle
import com.openreachtube.app.ui.components.TrendingCard
import com.openreachtube.app.ui.theme.ZRed

@Composable
fun HomeScreen(
    onOpenVideo: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        HomeHeader(onOpenSearch = onOpenSearch, onOpenSettings = onOpenSettings)
        CategoryChipRow(
            categories = state.categories,
            selected = state.selectedCategory,
            onSelect = viewModel::selectCategory,
        )
        when {
            state.loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.error != null -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Couldn't load the feed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    state.error ?: "Network error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                TextButton(onClick = viewModel::refresh, Modifier.padding(top = 8.dp)) {
                    Text("Retry")
                }
            }

            else -> LazyColumn(Modifier.fillMaxSize()) {
                if (state.continuePlaying.isNotEmpty()) {
                    item { SectionTitle("Continue Playing") }
                    item {
                        LazyRow(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.continuePlaying.size) { i ->
                                val video = state.continuePlaying[i]
                                RailCard(video) { onOpenVideo(video.id) }
                            }
                        }
                    }
                }
                item { SectionTitle("Trending Feed") }
                items(state.trending.size, key = { state.trending[it].id }) { i ->
                    val video = state.trending[i]
                    TrendingCard(video) { onOpenVideo(video.id) }
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }
    }
}

/** ZyvoTube-style header: red play logo + wordmark, search and account actions. */
@Composable
private fun HomeHeader(onOpenSearch: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ZRed),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        Row(Modifier.padding(start = 10.dp), verticalAlignment = Alignment.Bottom) {
            Text("OpenReach", color = ZRed, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Tube",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onOpenSearch) {
            Icon(Icons.Filled.Search, contentDescription = "Search")
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Person, contentDescription = "Account")
        }
    }
}
