package com.openreachtube.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesManager,
) : ViewModel() {

    val settings = preferences.settings

    fun setDefaultQuality(value: String) = viewModelScope.launch { preferences.setDefaultQuality(value) }
    fun setBackgroundPlayEnabled(value: Boolean) = viewModelScope.launch { preferences.setBackgroundPlay(value) }
    fun setSponsorBlockEnabled(value: Boolean) = viewModelScope.launch { preferences.setSponsorBlock(value) }
    fun setProxy(value: String?) = viewModelScope.launch { preferences.setProxy(value) }
}
