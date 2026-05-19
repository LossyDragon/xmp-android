package com.lossydragon.media3.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import timber.log.Timber

private val Context.dataStore by preferencesDataStore(
    name = "xmp_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler {
        Timber.e("Preferences corrupted, resetting.")
        emptyPreferences()
    }
)

class XmpPreferences(context: Context) {

    private val dataStore: DataStore<Preferences> = context.dataStore

    private object Prefs {
        val LAST_DIRECTORY_URI = stringPreferencesKey("last_directory_uri")
    }

    private fun <T> flow(key: Preferences.Key<T>, default: T): Flow<T> =
        dataStore.data.map { it[key] ?: default }

    private fun <T> flowNullable(key: Preferences.Key<T>): Flow<T?> =
        dataStore.data.map { it[key] }

    private suspend fun <T> get(key: Preferences.Key<T>, default: T): T =
        dataStore.data.map { it[key] ?: default }.firstOrNull() ?: default

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) =
        dataStore.edit { it[key] = value }

    fun getLastDirectoryFlow() = flowNullable(Prefs.LAST_DIRECTORY_URI)
    suspend fun getLastDirectoryUri() = get(Prefs.LAST_DIRECTORY_URI, "").ifEmpty { null }
    suspend fun setLastDirectoryUri(v: String) = set(Prefs.LAST_DIRECTORY_URI, v)
}
