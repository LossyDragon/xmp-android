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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.helllabs.android.xmp.model.Module
import timber.log.Timber

object PrefManager {

    private val Context.dataStore by preferencesDataStore(
        name = "preferences",
        corruptionHandler = ReplaceFileCorruptionHandler {
            Timber.e("Preferences corrupted, resetting.")
            emptyPreferences()
        }
    )

    private lateinit var dataStore: DataStore<Preferences>

    fun init(context: Context) {
        dataStore = context.dataStore
    }

    fun clearPreferences() {
        runBlocking {
            dataStore.edit { it.clear() }
        }
    }

    private fun <T> getPref(key: Preferences.Key<T>, defaultValue: T): T = runBlocking {
        dataStore.data.first()[key] ?: defaultValue
    }

    private fun <T> setPref(key: Preferences.Key<T>, value: T) {
        runBlocking {
            dataStore.edit { pref -> pref[key] = value }
        }
    }

    @Suppress("SameParameterValue")
    private fun <T> removePref(key: Preferences.Key<T>) {
        runBlocking {
            dataStore.edit { pref -> pref.remove(key) }
        }
    }

    private val SAF_PATH = stringPreferencesKey("saf_storage_path")
    var safStoragePath: String
        get() = getPref(SAF_PATH, "")
        set(value) {
            setPref(SAF_PATH, value)
        }

    /**
     * 1: Start playing at selection
     * 2: Play selected file
     * 3: Enqueue selected file
     */
    private val PLAYLIST_MODE = intPreferencesKey("playlist_mode")
    var playlistMode: Int
        get() = getPref(PLAYLIST_MODE, 1)
        set(value) {
            setPref(PLAYLIST_MODE, value)
        }

    private val ALL_SEQUENCES = booleanPreferencesKey("all_sequences")
    var allSequences: Boolean
        get() = getPref(ALL_SEQUENCES, false)
        set(value) {
            setPref(ALL_SEQUENCES, value)
        }

    private val SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    var keepScreenOn: Boolean
        get() = getPref(SCREEN_ON, false)
        set(value) {
            setPref(SCREEN_ON, value)
        }

    private val SHOW_INFO_LINE = booleanPreferencesKey("show_info_line")
    var showInfoLine: Boolean
        get() = getPref(SHOW_INFO_LINE, true)
        set(value) {
            setPref(SHOW_INFO_LINE, value)
        }

    private val USE_FILENAME = booleanPreferencesKey("use_filename")
    var useFileName: Boolean
        get() = getPref(USE_FILENAME, false)
        set(value) {
            setPref(USE_FILENAME, value)
        }

    private val INSTALL_EXAMPLE_PLAYLIST = booleanPreferencesKey("example_playlist_created")
    var installedExamplePlaylist: Boolean
        get() = getPref(INSTALL_EXAMPLE_PLAYLIST, false)
        set(value) {
            setPref(INSTALL_EXAMPLE_PLAYLIST, value)
        }

    private val INSTALL_EXAMPLES = booleanPreferencesKey("examples")
    var examples: Boolean
        get() = getPref(INSTALL_EXAMPLES, true)
        set(value) {
            setPref(INSTALL_EXAMPLES, value)
        }

    private val BACK_BUTTON = booleanPreferencesKey("back_button_navigation")
    var backButtonNavigation: Boolean
        get() = getPref(BACK_BUTTON, true)
        set(value) {
            setPref(BACK_BUTTON, value)
        }

    private val SHUFFLE_MODE = booleanPreferencesKey("options_shuffleMode")
    var shuffleMode: Boolean
        get() = getPref(SHUFFLE_MODE, true)
        set(value) {
            setPref(SHUFFLE_MODE, value)
        }

    private val LOOP_MODE = booleanPreferencesKey("options_loopMode")
    var loopMode: Boolean
        get() = getPref(LOOP_MODE, false)
        set(value) {
            setPref(LOOP_MODE, value)
        }

    private val MODARCHIVE_FOLDER = booleanPreferencesKey("modarchive_folder")
    var modArchiveFolder: Boolean
        get() = getPref(MODARCHIVE_FOLDER, true)
        set(value) {
            setPref(MODARCHIVE_FOLDER, value)
        }

    private val ARTIST_FOLDER = booleanPreferencesKey("artist_folder")
    var artistFolder: Boolean
        get() = getPref(ARTIST_FOLDER, true)
        set(value) {
            setPref(ARTIST_FOLDER, value)
        }

    private val BUFFER_MS = intPreferencesKey("buffer_ms_opensl")
    var bufferMs: Int
        get() = getPref(BUFFER_MS, 400)
        set(value) {
            setPref(BUFFER_MS, value)
        }

    private val SAMPLE_RATE = intPreferencesKey("sampling_rate")
    var samplingRate: Int
        get() = getPref(SAMPLE_RATE, 44100)
        set(value) {
            setPref(SAMPLE_RATE, value)
        }

    private val DEFAULT_PAN = intPreferencesKey("default_pan")
    var defaultPan: Int
        get() = getPref(DEFAULT_PAN, 50)
        set(value) {
            setPref(DEFAULT_PAN, value)
        }

    private val VOLUME_BOOST = intPreferencesKey("vol_boost")
    var volumeBoost: Int
        get() = getPref(VOLUME_BOOST, 1)
        set(value) {
            setPref(VOLUME_BOOST, value)
        }

    /**
     * 1: Linear
     * 2: Cubic spline
     */
    private val INTERP_TYPE = intPreferencesKey("interp_type")
    var interpType: Int
        get() = getPref(INTERP_TYPE, 1)
        set(value) {
            setPref(INTERP_TYPE, value)
        }

    private val INTERPOLATE = booleanPreferencesKey("interpolate")
    var interpolate: Boolean
        get() = getPref(INTERPOLATE, true)
        set(value) {
            setPref(INTERPOLATE, value)
        }

    private val STEREO_MIX = intPreferencesKey("stereo_mix")
    var stereoMix: Int
        get() = getPref(STEREO_MIX, 100)
        set(value) {
            setPref(STEREO_MIX, value)
        }

    private val AMIGA_MIXER = booleanPreferencesKey("amiga_mixer")
    var amigaMixer: Boolean
        get() = getPref(AMIGA_MIXER, false)
        set(value) {
            setPref(AMIGA_MIXER, value)
        }

    private val SEARCH_HISTORY = stringPreferencesKey("search_history")
    var searchHistory: List<Module>
        get() {
            val string = getPref(SEARCH_HISTORY, "[]")
            var list: List<Module>
            try {
                list = Json.decodeFromString(string)
            } catch (e: Exception) {
                Timber.e("Error getting search history")
                removePref(SEARCH_HISTORY)
                list = listOf()
            }
            return list
        }
        set(value) {
            var json: String
            try {
                json = Json.encodeToString(value)
            } catch (e: Exception) {
                Timber.e("Error setting search history")
                removePref(SEARCH_HISTORY)
                json = ""
            }
            setPref(SEARCH_HISTORY, json)
        }

    private val SHOW_HEX = booleanPreferencesKey("player_show_hex")
    var showHex: Boolean
        get() = getPref(SHOW_HEX, false)
        set(value) {
            setPref(SHOW_HEX, value)
        }
}
