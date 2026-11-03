package com.openreachtube.app.ui.screens.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.local.PreferencesManager
import com.openreachtube.app.data.network.UpdateInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UpdateUiState(
    val checking: Boolean = false,
    val message: String? = null,
    val staged: UpdateInstaller.StagedUpdate? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesManager,
    private val updateInstaller: UpdateInstaller,
) : ViewModel() {

    val settings = preferences.settings

    private val _updateState = MutableStateFlow(UpdateUiState())
    val updateState: StateFlow<UpdateUiState> = _updateState

    fun setDefaultQuality(value: String) = viewModelScope.launch { preferences.setDefaultQuality(value) }
    fun setBackgroundPlayEnabled(value: Boolean) = viewModelScope.launch { preferences.setBackgroundPlay(value) }
    fun setSponsorBlockEnabled(value: Boolean) = viewModelScope.launch { preferences.setSponsorBlock(value) }
    fun setProxy(value: String?) = viewModelScope.launch { preferences.setProxy(value) }

    /** Polls the GitHub releases feed and stages the APK when a new tag exists. */
    fun checkForUpdates() {
        if (_updateState.value.checking) return
        _updateState.value = UpdateUiState(checking = true)
        viewModelScope.launch {
            updateInstaller.checkAndStage().fold(
                onSuccess = { staged ->
                    _updateState.value = if (staged == null) {
                        UpdateUiState(message = "You're on the latest published version.")
                    } else {
                        UpdateUiState(
                            staged = staged,
                            message = "Version ${staged.tag} is ready to install.",
                        )
                    }
                },
                onFailure = {
                    _updateState.value =
                        UpdateUiState(message = "Update check failed: ${it.message}")
                },
            )
        }
    }

    /** Opens the staged APK through the Android package installer. */
    fun installIntent(staged: UpdateInstaller.StagedUpdate): Intent =
        updateInstaller.installIntent(staged.apk)
}
