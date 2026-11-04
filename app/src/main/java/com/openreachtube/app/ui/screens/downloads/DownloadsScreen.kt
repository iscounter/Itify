package com.openreachtube.app.ui.screens.downloads

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.local.dao.MissionDao
import com.openreachtube.app.data.local.entity.DownloadMissionEntity
import com.openreachtube.app.ui.theme.ZTextMuted
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Where DownloadServiceBase writes finished media ({missionId}.mp4/.m4a). */
fun downloadFile(context: Context, mission: DownloadMissionEntity): File =
    File(
        File(context.filesDir, "downloads"),
        mission.missionId + if (mission.isAudioOnly) ".m4a" else ".mp4",
    )

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val missionDao: MissionDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val missions: StateFlow<List<DownloadMissionEntity>> = missionDao.allAsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Deletes the media file and its mission row; tolerates missing files. */
    fun delete(mission: DownloadMissionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                downloadFile(context, mission).delete()
                missionDao.delete(mission.missionId)
            }
        }
    }

    /** ACTION_VIEW for the finished media, or null when the file is missing. */
    fun openIntent(mission: DownloadMissionEntity): Intent? {
        val file = downloadFile(context, mission)
        if (!file.exists()) return null
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, if (mission.isAudioOnly) "audio/mp4" else "video/mp4")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val missions by viewModel.missions.collectAsState()
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Downloads",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack) { Text("Done") }
        }

        if (missions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No downloads yet.\nTap Download on any video to save it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZTextMuted,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(missions.size, key = { missions[it].missionId }) { i ->
                    val mission = missions[i]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.openIntent(mission)?.let { context.startActivity(it) }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                mission.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                            )
                            Text(
                                listOf(
                                    if (mission.isAudioOnly) "Audio" else "Video",
                                    mission.status,
                                ).joinToString("  ·  "),
                                style = MaterialTheme.typography.bodySmall,
                                color = ZTextMuted,
                            )
                        }
                        if (mission.status == "DONE") {
                            TextButton(onClick = {
                                viewModel.openIntent(mission)?.let { context.startActivity(it) }
                            }) { Text("Play") }
                        }
                        IconButton(onClick = { viewModel.delete(mission) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete download",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }
    }
}
