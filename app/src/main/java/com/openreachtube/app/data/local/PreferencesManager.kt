package com.openreachtube.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "openreach_settings")

data class AppSettings(
    val defaultQuality: String = "auto",
    val backgroundPlay: Boolean = true,
    val sponsorBlock: Boolean = true,
    val proxy: String? = null,
)

/** Jetpack DataStore-backed settings (same mechanism family as the teardown). */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val QUALITY = stringPreferencesKey("default_quality")
        val BACKGROUND = booleanPreferencesKey("background_play")
        val SPONSOR = booleanPreferencesKey("sponsor_block")
        val PROXY = stringPreferencesKey("proxy")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            defaultQuality = prefs[Keys.QUALITY] ?: "auto",
            backgroundPlay = prefs[Keys.BACKGROUND] ?: true,
            sponsorBlock = prefs[Keys.SPONSOR] ?: true,
            proxy = prefs[Keys.PROXY],
        )
    }

    suspend fun setDefaultQuality(value: String) = context.dataStore.edit { it[Keys.QUALITY] = value }
    suspend fun setBackgroundPlay(value: Boolean) = context.dataStore.edit { it[Keys.BACKGROUND] = value }
    suspend fun setSponsorBlock(value: Boolean) = context.dataStore.edit { it[Keys.SPONSOR] = value }
    suspend fun setProxy(value: String?) = context.dataStore.edit {
        if (value == null) it.remove(Keys.PROXY) else it[Keys.PROXY] = value
    }
}
