package org.helllabs.android.xmp.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import de.schnettler.datastore.manager.DataStoreManager
import de.schnettler.datastore.manager.PreferenceRequest
import kotlinx.coroutines.flow.Flow
import org.helllabs.android.xmp.ui.MainActivity

object PrefManager {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    lateinit var dataStoreManager: DataStoreManager
        private set

    internal val prefThemeItems = mapOf(
        "light" to "Light",
        "dark" to "Dark",
        "auto" to "Auto (Default)"
    )
    internal val prefSamplingRate = mapOf(
        "8000" to "8kHz",
        "22050" to "22kHz",
        "44100" to "44.1kHz",
        "48000" to "48kHz"
    )
    internal val prefVolumeBoost = mapOf(
        "1" to "1x (Normal)",
        "2" to "2x",
        "3" to "4x",
    )
    internal val prefInterpolationType = mapOf(
        "1" to "Linear",
        "2" to "Cubic spline"
    )

    private const val PREF_ALL_SEQUENCES = "all_sequences"
    private const val PREF_AMIGA_MIXER = "amiga_mixer"
    private const val PREF_ARTIST_FOLDER = "artist_folder"
    private const val PREF_BUFFER_MS = "buffer_ms_opensl"
    private const val PREF_CHANGELOG_VERSION = "changelog_version"
    private const val PREF_DEFAULT_PAN = "default_pan"
    private const val PREF_INSTALL_EXAMPLES = "pref_install_examples"
    private const val PREF_INTERPOLATE = "interpolate"
    private const val PREF_INTERP_TYPE = "interp_type"
    private const val PREF_KEEP_SCREEN_ON = "keep_screen_on"
    private const val PREF_NEW_NOTIFICATION = "pref_use_newer_notification"
    private const val PREF_NEW_WAVEFORM = "use_new_waveform"
    private const val PREF_SAMPLING_RATE = "sampling_rate"
    private const val PREF_SHOW_INFO_LINE = "show_info_line"
    private const val PREF_SHOW_INFO_LINE_HEX = "show_info_line_hex"
    private const val PREF_START_ON_PLAYER = "start_on_player"
    private const val PREF_STEREO_MIX = "stereo_mix"
    private const val PREF_THEME = "pref_theme"
    private const val PREF_TMA_FOLDER = "modarchive_folder"
    private const val PREF_USE_FILENAME = "use_filename"
    private const val PREF_VOL_BOOST = "vol_boost"
    private const val PREF_LOOP_MODE = "options_loopMode"
    private const val PREF_SHUFFLE_MODE = "options_shuffleMode"
    private const val PREF_MEDIA_PATH = "media_path"
    private const val PREF_SEARCH_HISTORY = "search_history"

    fun init(context: Context) {
        dataStoreManager = DataStoreManager(context.dataStore)
    }

    val searchHistoryRequest = PreferenceRequest(
        key = stringPreferencesKey(PREF_SEARCH_HISTORY),
        defaultValue = ""
    )

    val mediaPathRequest = PreferenceRequest(
        key = stringPreferencesKey(PREF_MEDIA_PATH),
        defaultValue = MainActivity.DEFAULT_MEDIA_PATH
    )

    val loopModeRequest = PreferenceRequest(
        key = booleanPreferencesKey(PREF_LOOP_MODE),
        defaultValue = false
    )

    val shuffleModeRequest = PreferenceRequest(
        key = booleanPreferencesKey(PREF_SHUFFLE_MODE),
        defaultValue = false
    )

    val amigaMixerRequest = PreferenceRequest(
        key = booleanPreferencesKey(PREF_AMIGA_MIXER),
        defaultValue = false
    )

    val bufferSizeRequest = PreferenceRequest(
        key = floatPreferencesKey(PREF_BUFFER_MS),
        defaultValue = 400f
    )

    val changeLogRequest = PreferenceRequest(
        key = intPreferencesKey(PREF_CHANGELOG_VERSION),
        defaultValue = 0
    )

    val defaultPanRequest = PreferenceRequest(
        key = floatPreferencesKey(PREF_DEFAULT_PAN),
        defaultValue = 50f
    )

    val hiddenPatternsRequest = PreferenceRequest(
        key = booleanPreferencesKey(PREF_ALL_SEQUENCES),
        defaultValue = false
    )

    val installExamplesRequest = PreferenceRequest(
        key = booleanPreferencesKey(PREF_INSTALL_EXAMPLES),
        defaultValue = true
    )

    val interpolationRequest = PreferenceRequest(
        key = booleanPreferencesKey(PREF_INTERPOLATE),
        defaultValue = false
    )

    val interpolationTypeRequest = PreferenceRequest(
        key = stringPreferencesKey(PREF_INTERP_TYPE),
        defaultValue = "1"
    )

    val keepScreenOnRequest = PreferenceRequest(
        key = booleanPreferencesKey(PREF_KEEP_SCREEN_ON),
        defaultValue = false
    )

    val launchInPlayerRequest = PreferenceRequest(
        key = booleanPreferencesKey(PREF_START_ON_PLAYER),
        defaultValue = true
    )

    val showInfoLineRequest = PreferenceRequest(
        key = booleanPreferencesKey(PREF_SHOW_INFO_LINE),
        defaultValue = true
    )

    val samplingRateRequest = PreferenceRequest(
        key = stringPreferencesKey(PREF_SAMPLING_RATE),
        "44100"
    )

    val showHexValuesRequest = PreferenceRequest(
        key = booleanPreferencesKey(PREF_SHOW_INFO_LINE_HEX),
        defaultValue = true
    )

    val stereoSeparationRequest = PreferenceRequest(
        key = floatPreferencesKey(PREF_STEREO_MIX),
        defaultValue = 100f
    )

    val themeRequest = PreferenceRequest(
        key = stringPreferencesKey(PREF_THEME),
        defaultValue = "auto"
    )

    val useArtistFolderRequest = PreferenceRequest(
        key = booleanPreferencesKey(PREF_ARTIST_FOLDER),
        defaultValue = true
    )

    val useBetterWaveformRequest = PreferenceRequest(
        key = booleanPreferencesKey(PREF_NEW_WAVEFORM),
        defaultValue = true
    )

    val useFileNamesRequest = PreferenceRequest(
        key = booleanPreferencesKey(PREF_USE_FILENAME),
        defaultValue = false
    )

    val useMediaStyleNotificationRequest = PreferenceRequest(
        key = booleanPreferencesKey(PREF_NEW_NOTIFICATION),
        defaultValue = true
    )

    val useTmaFolderRequest = PreferenceRequest(
        key = booleanPreferencesKey(PREF_TMA_FOLDER),
        defaultValue = true
    )

    val volumeBoostRequest = PreferenceRequest(
        key = stringPreferencesKey(PREF_VOL_BOOST),
        defaultValue = "1"
    )

    suspend fun <T> getPreference(request: PreferenceRequest<T>): T {
        return dataStoreManager.getPreference(request)
    }

    fun <T> getPreferenceFlow(request: PreferenceRequest<T>): Flow<T> {
        return dataStoreManager.getPreferenceFlow(request)
    }
}
