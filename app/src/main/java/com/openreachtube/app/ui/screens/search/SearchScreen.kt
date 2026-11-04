package com.openreachtube.app.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.openreachtube.app.domain.model.SearchTab
import com.openreachtube.app.ui.components.VideoRow
import com.openreachtube.app.ui.theme.ZTextMuted

@Composable
fun SearchScreen(
    initialQuery: String,
    onOpenVideo: (String) -> Unit = {},
    onOpenChannel: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChanged,
            label = { Text("Search OpenReach Tube") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchTab.entries.forEach { tab ->
                FilterChip(
                    selected = state.tab == tab,
                    onClick = { viewModel.onTabChanged(tab) },
                    label = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }
        if (state.recentSearches.isNotEmpty() && state.results.isEmpty()) {
            Text(
                "Recent searches",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            state.recentSearches.take(5).forEach { recent ->
                TextButton(onClick = { viewModel.onQueryChanged(recent) }) { Text(recent) }
            }
        }
        if (state.loading) {
            Row(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
        }
        if (!state.loading && state.query.isNotBlank() && state.results.isEmpty()) {
            Text(
                "No results.",
                style = MaterialTheme.typography.bodyMedium,
                color = ZTextMuted,
                modifier = Modifier.padding(16.dp),
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(state.results.size, key = { state.results[it].id }) { i ->
                val result = state.results[i]
                VideoRow(result) {
                    if (result.isChannel) onOpenChannel(result.id) else onOpenVideo(result.id)
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }
}
