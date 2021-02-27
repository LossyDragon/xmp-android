package org.helllabs.android.xmp.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager

object PrefManager {

    // Don't use PREF_INTERPOLATION -- was boolean in 2.x and string in 3.2.0

    // change the variable name so we can use the new default mix value
    // public static final String BACK_BUTTON_PARENTDIR = "back_button_parentdir";
    // public static final String BUFFER_MS = "buffer_ms";
    // public static final String FILTER = "filter";
    // public static final String PAN_SEPARATION = "pan_separation";
    // public static final String STEREO = "stereo";
    // public static final String TITLES_IN_BROWSER = "titles_in_browser";
    // private const val BACK_BUTTON_NAVIGATION = "back_button_navigation"

    private const val ALL_SEQUENCES = "all_sequences"
    private const val AMIGA_MIXER = "amiga_mixer"
    private const val ARTIST_FOLDER = "artist_folder"
    private const val BLUETOOTH_PAUSE = "bluetooth_pause"
    private const val BUFFER_MS = "buffer_ms_opensl"
    private const val CHANGELOG_VERSION = "changelog_version"
    private const val DEFAULT_PAN = "default_pan"
    private const val ENABLE_DELETE = "enable_delete"
    private const val EXAMPLES = "examples"
    private const val HEADSET_PAUSE = "headset_pause"
    private const val INTERPOLATE = "interpolate"
    private const val INTERP_TYPE = "interp_type"
    private const val KEEP_SCREEN_ON = "keep_screen_on"
    private const val MEDIA_PATH = "media_path"
    private const val MODARCHIVE_FOLDER = "modarchive_folder"
    private const val PLAYLIST_MODE = "playlist_mode"
    private const val SAMPLING_RATE = "sampling_rate"
    private const val SHOW_INFO_LINE = "show_info_line"
    private const val SHOW_TOAST = "show_toast"
    private const val START_ON_PLAYER = "start_on_player"
    private const val STEREO_MIX = "stereo_mix"
    private const val USE_FILENAME = "use_filename"
    private const val VOL_BOOST = "vol_boost"
    private const val NEW_WAVEFORM = "use_new_waveform"

    // New
    private const val SEARCH_HISTORY = "search_history"
    private const val SHOW_INFO_LINE_HEX = "show_info_line_hex"
    private const val NEW_NOTIFICATION = "pref_use_newer_notification"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun getBooleanPref(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    fun setBooleanPref(key: String, defaultValue: Boolean) {
        prefs.edit { putBoolean(key, defaultValue) }
    }

    fun removeBooleanPref(key: String) {
        prefs.edit { remove(key) }
    }

    fun clearSearchHistory() {
        prefs.edit { remove(SEARCH_HISTORY) }
    }

    var showToast: Boolean
        get() = prefs.getBoolean(SHOW_TOAST, true)
        set(value) = prefs.edit { putBoolean(SHOW_TOAST, value) }

    var playlistMode: String
        get() = prefs.getString(PLAYLIST_MODE, "1")!!
        set(value) = prefs.edit { putString(PLAYLIST_MODE, value) }

    var mediaPath: String
        get() = prefs.getString(MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH)!!
        set(value) = prefs.edit { putString(MEDIA_PATH, value) }

    // Not used in code anymore
    // var backButtonNavigation: Boolean
    //     get() = prefs.getBoolean(BACK_BUTTON_NAVIGATION, true)
    //     set(value) = prefs.edit { putBoolean(BACK_BUTTON_NAVIGATION, value) }

    var installExamples: Boolean
        get() = prefs.getBoolean(EXAMPLES, true)
        set(value) = prefs.edit { putBoolean(EXAMPLES, value) }

    var useFilename: Boolean
        get() = prefs.getBoolean(USE_FILENAME, false)
        set(value) = prefs.edit { putBoolean(USE_FILENAME, value) }

    var useModArchiveFolder: Boolean
        get() = prefs.getBoolean(MODARCHIVE_FOLDER, true)
        set(value) = prefs.edit { putBoolean(MODARCHIVE_FOLDER, value) }

    var useArtistFolder: Boolean
        get() = prefs.getBoolean(ARTIST_FOLDER, true)
        set(value) = prefs.edit { putBoolean(ARTIST_FOLDER, value) }

    var showInfoLine: Boolean
        get() = prefs.getBoolean(SHOW_INFO_LINE, true)
        set(value) = prefs.edit { putBoolean(SHOW_INFO_LINE, value) }

    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEEP_SCREEN_ON, false)
        set(value) = prefs.edit { putBoolean(KEEP_SCREEN_ON, value) }

    var enableDelete: Boolean
        get() = prefs.getBoolean(ENABLE_DELETE, false)
        set(value) = prefs.edit { putBoolean(ENABLE_DELETE, value) }

    var allSequences: Boolean
        get() = prefs.getBoolean(ALL_SEQUENCES, false)
        set(value) = prefs.edit { putBoolean(ALL_SEQUENCES, value) }

    var changelogVersion: Int
        get() = prefs.getInt(CHANGELOG_VERSION, 0)
        set(value) = prefs.edit { putInt(CHANGELOG_VERSION, value) }

    var stereoMix: Int
        get() = prefs.getInt(STEREO_MIX, 100)
        set(value) = prefs.edit { putInt(STEREO_MIX, value) }

    var amigaMixer: Boolean
        get() = prefs.getBoolean(AMIGA_MIXER, false)
        set(value) = prefs.edit { putBoolean(AMIGA_MIXER, value) }

    var volumeBoost: String
        get() = prefs.getString(VOL_BOOST, "1")!!
        set(value) = prefs.edit { putString(VOL_BOOST, value) }

    var interpType: String
        get() = prefs.getString(INTERP_TYPE, "1")!!
        set(value) = prefs.edit { putString(INTERP_TYPE, value) }

    var interpolate: Boolean
        get() = prefs.getBoolean(INTERPOLATE, true)
        set(value) = prefs.edit { putBoolean(INTERPOLATE, value) }

    var defaultPan: Int
        get() = prefs.getInt(DEFAULT_PAN, 50)
        set(value) = prefs.edit { putInt(DEFAULT_PAN, value) }

    var samplingRate: String
        get() = prefs.getString(SAMPLING_RATE, "44100")!!
        set(value) = prefs.edit { putString(SAMPLING_RATE, value) }

    var bufferMs: Int
        get() = prefs.getInt(BUFFER_MS, 400)
        set(value) = prefs.edit { putInt(BUFFER_MS, value) }

    var startOnPlayer: Boolean
        get() = prefs.getBoolean(START_ON_PLAYER, true)
        set(value) = prefs.edit { putBoolean(START_ON_PLAYER, value) }

    var headsetPause: Boolean
        get() = prefs.getBoolean(HEADSET_PAUSE, true)
        set(value) = prefs.edit { putBoolean(HEADSET_PAUSE, value) }

    var bluetoothPause: Boolean
        get() = prefs.getBoolean(BLUETOOTH_PAUSE, true)
        set(value) = prefs.edit { putBoolean(BLUETOOTH_PAUSE, value) }

    var useNewWaveform: Boolean
        get() = prefs.getBoolean(NEW_WAVEFORM, false)
        set(value) = prefs.edit { putBoolean(NEW_WAVEFORM, value) }

    // Search History
    var searchHistory: String?
        get() = prefs.getString(SEARCH_HISTORY, null)
        set(value) = prefs.edit { putString(SEARCH_HISTORY, value) }

    // Show either hex or decimal in the info line for player activity
    var showInfoLineHex: Boolean
        get() = prefs.getBoolean(SHOW_INFO_LINE_HEX, true)
        set(value) = prefs.edit { putBoolean(SHOW_INFO_LINE_HEX, value) }

    // Use new MediaStyle notification
    var useMediaStyle: Boolean
        get() = prefs.getBoolean(NEW_NOTIFICATION, true)
        set(value) = prefs.edit { putBoolean(NEW_NOTIFICATION, value) }
}
