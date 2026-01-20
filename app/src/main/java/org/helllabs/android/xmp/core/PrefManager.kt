package org.helllabs.android.xmp.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.helllabs.android.xmp.model.Module
import timber.log.Timber

private val Context.dataStore by preferencesDataStore(
    name = "preferences",
    corruptionHandler = ReplaceFileCorruptionHandler {
        Timber.e("Preferences corrupted, resetting.")
        emptyPreferences()
    }
)

class PrefManager(context: Context, private val json: Json) {
    private val dataStore: DataStore<Preferences> = context.dataStore

    private object Keys {
        val INITIAL_START_DESTINATION = stringPreferencesKey("initial_start_destination")

        val EXPLORER_ROOT_PATH = stringPreferencesKey("explorer_root_path")
        val PLAYLISTS_ROOT_PATH = stringPreferencesKey("playlists_root_path")

        val INSTALL_EXAMPLE_PLAYLIST = booleanPreferencesKey("example_playlist_created")

        val PLAYLIST_MODE = intPreferencesKey("playlist_mode")
        val ALL_SEQUENCES = booleanPreferencesKey("all_sequences")
        val SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SHOW_INFO_LINE = booleanPreferencesKey("show_info_line")
        val USE_FILENAME = booleanPreferencesKey("use_filename")

        val SHUFFLE_MODE = booleanPreferencesKey("options_shuffleMode")
        val LOOP_MODE = booleanPreferencesKey("options_loopMode")
        val MODARCHIVE_FOLDER = booleanPreferencesKey("modarchive_folder")
        val ARTIST_FOLDER = booleanPreferencesKey("artist_folder")
        val BUFFER_MS = intPreferencesKey("buffer_ms_opensl")
        val SAMPLE_RATE = intPreferencesKey("sampling_rate")
        val DEFAULT_PAN = intPreferencesKey("default_pan")
        val VOLUME_BOOST = intPreferencesKey("vol_boost")
        val INTERP_TYPE = intPreferencesKey("interp_type")
        val INTERPOLATE = booleanPreferencesKey("interpolate")
        val STEREO_MIX = intPreferencesKey("stereo_mix")
        val AMIGA_MIXER = booleanPreferencesKey("amiga_mixer")
        val SEARCH_HISTORY = stringPreferencesKey("search_history")
        val SHOW_HEX = booleanPreferencesKey("player_show_hex")
    }

    // Helper functions for async operations
    private fun <T> getFlow(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
        dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }

    private suspend fun <T> get(key: Preferences.Key<T>, defaultValue: T): T =
        dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }.firstOrNull() ?: defaultValue

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    suspend fun clearPreferences() {
        dataStore.edit { it.clear() }
    }

    suspend fun getInitialStart(): String = get(Keys.INITIAL_START_DESTINATION, "Playlists")
    fun initialStartFlow(): Flow<String> = getFlow(Keys.INITIAL_START_DESTINATION, "Playlists")
    suspend fun setInitialStart(value: String) = set(Keys.INITIAL_START_DESTINATION, value)

    suspend fun getExplorerRootPath(): String = get(Keys.EXPLORER_ROOT_PATH, "")
    suspend fun setExplorerRootPath(value: String) = set(Keys.EXPLORER_ROOT_PATH, value)

    suspend fun getPlaylistRootPath(): String = get(Keys.PLAYLISTS_ROOT_PATH, "")
    suspend fun setPlaylistRootPath(value: String) = set(Keys.PLAYLISTS_ROOT_PATH, value)
    fun flowPlaylistRootPath() = getFlow(Keys.PLAYLISTS_ROOT_PATH, "")

    /**
     * 1: Start playing at selection
     * 2: Play selected file
     * 3: Enqueue selected file
     */
    suspend fun getPlaylistMode(): Int = get(Keys.PLAYLIST_MODE, 1)
    suspend fun setPlaylistMode(value: Int) = set(Keys.PLAYLIST_MODE, value)
    fun playlistModeFlow(): Flow<Int> = getFlow(Keys.PLAYLIST_MODE, 1)

    suspend fun getAllSequences(): Boolean = get(Keys.ALL_SEQUENCES, false)
    suspend fun setAllSequences(value: Boolean) = set(Keys.ALL_SEQUENCES, value)
    fun allSequencesFlow(): Flow<Boolean> = getFlow(Keys.ALL_SEQUENCES, false)

    suspend fun getKeepScreenOn(): Boolean = get(Keys.SCREEN_ON, false)
    suspend fun setKeepScreenOn(value: Boolean) = set(Keys.SCREEN_ON, value)
    fun keepScreenOnFlow(): Flow<Boolean> = getFlow(Keys.SCREEN_ON, false)

    suspend fun getShowInfoLine(): Boolean = get(Keys.SHOW_INFO_LINE, true)
    suspend fun setShowInfoLine(value: Boolean) = set(Keys.SHOW_INFO_LINE, value)
    fun showInfoLineFlow(): Flow<Boolean> = getFlow(Keys.SHOW_INFO_LINE, true)

    suspend fun getUseFileName(): Boolean = get(Keys.USE_FILENAME, false)
    suspend fun setUseFileName(value: Boolean) = set(Keys.USE_FILENAME, value)
    fun useFileNameFlow(): Flow<Boolean> = getFlow(Keys.USE_FILENAME, false)

    suspend fun getInstalledExamplePlaylist(): Boolean = get(Keys.INSTALL_EXAMPLE_PLAYLIST, false)
    suspend fun setInstalledExamplePlaylist(value: Boolean) =
        set(Keys.INSTALL_EXAMPLE_PLAYLIST, value)

    fun installedExamplePlaylistFlow(): Flow<Boolean> = getFlow(
        Keys.INSTALL_EXAMPLE_PLAYLIST,
        false
    )

    suspend fun getShuffleMode(): Boolean = get(Keys.SHUFFLE_MODE, true)
    suspend fun setShuffleMode(value: Boolean) = set(Keys.SHUFFLE_MODE, value)
    fun shuffleModeFlow(): Flow<Boolean> = getFlow(Keys.SHUFFLE_MODE, true)

    suspend fun getLoopMode(): Boolean = get(Keys.LOOP_MODE, false)
    suspend fun setLoopMode(value: Boolean) = set(Keys.LOOP_MODE, value)
    fun loopModeFlow(): Flow<Boolean> = getFlow(Keys.LOOP_MODE, false)

    suspend fun getModArchiveFolder(): Boolean = get(Keys.MODARCHIVE_FOLDER, true)
    suspend fun setModArchiveFolder(value: Boolean) = set(Keys.MODARCHIVE_FOLDER, value)
    fun modArchiveFolderFlow(): Flow<Boolean> = getFlow(Keys.MODARCHIVE_FOLDER, true)

    suspend fun getArtistFolder(): Boolean = get(Keys.ARTIST_FOLDER, true)
    suspend fun setArtistFolder(value: Boolean) = set(Keys.ARTIST_FOLDER, value)
    fun artistFolderFlow(): Flow<Boolean> = getFlow(Keys.ARTIST_FOLDER, true)

    suspend fun getInterpolate(): Boolean = get(Keys.INTERPOLATE, true)
    suspend fun setInterpolate(value: Boolean) = set(Keys.INTERPOLATE, value)
    fun interpolateFlow(): Flow<Boolean> = getFlow(Keys.INTERPOLATE, true)

    suspend fun getAmigaMixer(): Boolean = get(Keys.AMIGA_MIXER, false)
    suspend fun setAmigaMixer(value: Boolean) = set(Keys.AMIGA_MIXER, value)
    fun amigaMixerFlow(): Flow<Boolean> = getFlow(Keys.AMIGA_MIXER, false)

    suspend fun getShowHex(): Boolean = get(Keys.SHOW_HEX, false)
    suspend fun setShowHex(value: Boolean) = set(Keys.SHOW_HEX, value)
    fun showHexFlow(): Flow<Boolean> = getFlow(Keys.SHOW_HEX, false)

    // Integer preferences
    suspend fun getBufferMs(): Int = get(Keys.BUFFER_MS, 400)
    suspend fun setBufferMs(value: Int) = set(Keys.BUFFER_MS, value)
    fun bufferMsFlow(): Flow<Int> = getFlow(Keys.BUFFER_MS, 400)

    suspend fun getSamplingRate(): Int = get(Keys.SAMPLE_RATE, 44100)
    suspend fun setSamplingRate(value: Int) = set(Keys.SAMPLE_RATE, value)
    fun samplingRateFlow(): Flow<Int> = getFlow(Keys.SAMPLE_RATE, 44100)

    suspend fun getDefaultPan(): Int = get(Keys.DEFAULT_PAN, 50)
    suspend fun setDefaultPan(value: Int) = set(Keys.DEFAULT_PAN, value)
    fun defaultPanFlow(): Flow<Int> = getFlow(Keys.DEFAULT_PAN, 50)

    suspend fun getVolumeBoost(): Int = get(Keys.VOLUME_BOOST, 1)
    suspend fun setVolumeBoost(value: Int) = set(Keys.VOLUME_BOOST, value)
    fun volumeBoostFlow(): Flow<Int> = getFlow(Keys.VOLUME_BOOST, 1)

    /**
     * 1: Linear
     * 2: Cubic spline
     */
    suspend fun getInterpType(): Int = get(Keys.INTERP_TYPE, 1)
    suspend fun setInterpType(value: Int) = set(Keys.INTERP_TYPE, value)
    fun interpTypeFlow(): Flow<Int> = getFlow(Keys.INTERP_TYPE, 1)

    suspend fun getStereoMix(): Int = get(Keys.STEREO_MIX, 100)
    suspend fun setStereoMix(value: Int) = set(Keys.STEREO_MIX, value)
    fun stereoMixFlow(): Flow<Int> = getFlow(Keys.STEREO_MIX, 100)

    // Complex type - Search History
    suspend fun getSearchHistory(): ImmutableList<Module> {
        val string = get(Keys.SEARCH_HISTORY, "[]")
        return try {
            val list: List<Module> = json.decodeFromString(string)
            list.toPersistentList()
        } catch (e: Exception) {
            Timber.e(e, "Error getting search history")
            dataStore.edit { it.remove(Keys.SEARCH_HISTORY) }
            persistentListOf()
        }
    }

    suspend fun setSearchHistory(value: ImmutableList<Module>) {
        try {
            val jsonString = json.encodeToString(value)
            set(Keys.SEARCH_HISTORY, jsonString)
        } catch (e: Exception) {
            Timber.e(e, "Error setting search history")
            dataStore.edit { it.remove(Keys.SEARCH_HISTORY) }
        }
    }

    fun searchHistoryFlow(): Flow<ImmutableList<Module>> = dataStore.data.map { preferences ->
        val string = preferences[Keys.SEARCH_HISTORY] ?: "[]"
        try {
            val list: List<Module> = json.decodeFromString(string)
            list.toPersistentList()
        } catch (e: Exception) {
            Timber.e(e, "Error parsing search history")
            dataStore.edit { it.remove(Keys.SEARCH_HISTORY) }
            persistentListOf()
        }
    }
}
