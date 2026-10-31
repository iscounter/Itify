package com.openreachtube.app.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openreachtube.app.domain.model.SearchTab

@Composable
fun SearchScreen(initialQuery: String, viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                label = { Text("Search OpenReach Tube") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
        item {
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
        }
        items(state.results.size) { i ->
            Text(state.results[i].title, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
    }
}
