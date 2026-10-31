package com.openreachtube.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState(initial = null)
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Text("Background play", Modifier.padding(top = 12.dp))
        Switch(
            checked = settings?.backgroundPlay ?: true,
            onCheckedChange = viewModel::setBackgroundPlayEnabled,
        )
        Text("SponsorBlock segments")
        Switch(
            checked = settings?.sponsorBlock ?: true,
            onCheckedChange = viewModel::setSponsorBlockEnabled,
        )
        Text("Default quality: ${settings?.defaultQuality ?: "auto"}")
        Text(
            "This app streams and downloads content from third-party platforms. " +
                "You are responsible for complying with each platform's terms in your jurisdiction.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}
