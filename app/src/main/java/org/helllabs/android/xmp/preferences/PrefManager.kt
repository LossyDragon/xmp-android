package org.helllabs.android.xmp.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.helllabs.android.xmp.preferences.Preferences.Companion.DEFAULT_MEDIA_PATH
import org.helllabs.android.xmp.util.THEME_DEFAULT
import java.lang.Exception

object PrefManager {

    private const val ALL_SEQUENCES = "all_sequences"
    private const val AMIGA_MIXER = "amiga_mixer"
    private const val APP_THEME = "themePref"
    private const val ARTIST_FOLDER = "artist_folder"
    private const val BACK_BUTTON_NAVIGATION = "back_button_navigation"
    private const val BUFFER_MS = "buffer_ms_opensl"
    private const val CHANGELOG_VERSION = "changelog_version"
    private const val DEFAULT_PAN = "default_pan"
    private const val ENABLE_DELETE = "enable_delete"
    private const val EXAMPLES = "examples"
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
    private const val PLAYER_DRAG_LOCK = "player_drag_lock"
    private const val PLAYER_DRAG_STAY = "player_drag_stay"
    private const val OPTIONS_SHUFFLE_MODE = "options_shuffleMode"
    private const val OPTIONS_LOOP_MODE = "options_loopMode"
    private const val DEFAULT_BUFFER_VALUE = 400
    private const val DEFAULT_PAN_VALUE = 50
    private const val DEFAULT_SAMPLE_VALUE = "44100"
    private const val DEFAULT_BOOST_VALUE = "1"
    private const val DEFAULT_PLAYLIST_VALUE = "1"
    private const val DEFAULT_INTERP_VALUE = "1"
    private const val DEFAULT_STEREO_VALUE = 100
    private const val DEFAULT_SHUFFLE_MODE = true
    private const val DEFAULT_LOOP_MODE = false

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    private fun writePref(key: String, value: Any?) {
        if (value == null)
            throw Exception("Preference [$key] shouldn't be null!")

        val editor = prefs.edit()

        when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            else -> throw Exception("Something went wrong when saving [$key]")
        }

        editor.apply()
    }

    var allSequences: Boolean
        get() = prefs.getBoolean(ALL_SEQUENCES, false)
        set(value) {
            writePref(ALL_SEQUENCES, value)
        }

    var amigaMixer: Boolean
        get() = prefs.getBoolean(AMIGA_MIXER, false)
        set(value) {
            writePref(AMIGA_MIXER, value)
        }

    var themePref: String
        get() = prefs.getString(APP_THEME, THEME_DEFAULT)!!
        set(value) {
            writePref(APP_THEME, value)
        }

    var artistFolder: Boolean
        get() = prefs.getBoolean(ARTIST_FOLDER, true)
        set(value) {
            writePref(ARTIST_FOLDER, value)
        }

    var backButtonNavigation: Boolean
        get() = prefs.getBoolean(BACK_BUTTON_NAVIGATION, true)
        set(value) {
            writePref(BACK_BUTTON_NAVIGATION, value)
        }

    var bufferMS: Int
        get() = prefs.getInt(BUFFER_MS, DEFAULT_BUFFER_VALUE)
        set(value) {
            writePref(BUFFER_MS, value)
        }

    var changelogVersion: Int
        get() = prefs.getInt(CHANGELOG_VERSION, 0)
        set(value) {
            writePref(CHANGELOG_VERSION, value)
        }

    var defaultPan: Int
        get() = prefs.getInt(DEFAULT_PAN, DEFAULT_PAN_VALUE)
        set(value) {
            writePref(DEFAULT_PAN, value)
        }

    var enableDelete: Boolean
        get() = prefs.getBoolean(ENABLE_DELETE, false)
        set(value) {
            writePref(ENABLE_DELETE, value)
        }

    var installExample: Boolean
        get() = prefs.getBoolean(EXAMPLES, true)
        set(value) {
            writePref(EXAMPLES, value)
        }

    var interpolate: Boolean
        get() = prefs.getBoolean(INTERPOLATE, true)
        set(value) {
            writePref(INTERPOLATE, value)
        }

    var interpType: String // Needs to be a string
        get() = prefs.getString(INTERP_TYPE, DEFAULT_INTERP_VALUE)!!
        set(value) {
            writePref(INTERP_TYPE, value)
        }

    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEEP_SCREEN_ON, false)
        set(value) {
            writePref(KEEP_SCREEN_ON, value)
        }

    var mediaPath: String
        get() = prefs.getString(MEDIA_PATH, DEFAULT_MEDIA_PATH)!!
        set(value) {
            writePref(MEDIA_PATH, value)
        }

    var modArchiveFolder: Boolean
        get() = prefs.getBoolean(MODARCHIVE_FOLDER, true)
        set(value) {
            writePref(MODARCHIVE_FOLDER, value)
        }

    var playlistMode: String // Needs to be string
        get() = prefs.getString(PLAYLIST_MODE, DEFAULT_PLAYLIST_VALUE)!!
        set(value) {
            writePref(PLAYLIST_MODE, value)
        }

    var samplingRate: String // Needs to be string
        get() = prefs.getString(SAMPLING_RATE, DEFAULT_SAMPLE_VALUE)!!
        set(value) {
            writePref(SAMPLING_RATE, value)
        }

    var showInfoLine: Boolean
        get() = prefs.getBoolean(SHOW_INFO_LINE, true)
        set(value) {
            writePref(SHOW_INFO_LINE, value)
        }

    var showToast: Boolean
        get() = prefs.getBoolean(SHOW_TOAST, true)
        set(value) {
            writePref(SHOW_TOAST, value)
        }

    var startOnPlayer: Boolean
        get() = prefs.getBoolean(START_ON_PLAYER, true)
        set(value) {
            writePref(START_ON_PLAYER, value)
        }

    var stereoMix: Int
        get() = prefs.getInt(STEREO_MIX, DEFAULT_STEREO_VALUE)
        set(value) {
            writePref(STEREO_MIX, value)
        }

    var useFilename: Boolean
        get() = prefs.getBoolean(USE_FILENAME, false)
        set(value) {
            writePref(USE_FILENAME, value)
        }

    var volumeBoost: String // Needs to be String
        get() = prefs.getString(VOL_BOOST, DEFAULT_BOOST_VALUE)!!
        set(value) {
            writePref(VOL_BOOST, value)
        }

    var playerDragLock: Boolean
        get() = prefs.getBoolean(PLAYER_DRAG_LOCK, false)
        set(value) {
            writePref(PLAYER_DRAG_LOCK, value)
        }

    var playerDragStay: Boolean
        get() = prefs.getBoolean(PLAYER_DRAG_STAY, false)
        set(value) {
            writePref(PLAYER_DRAG_STAY, value)
        }

    var optionsModeShuffle: Boolean
        get() = prefs.getBoolean(OPTIONS_SHUFFLE_MODE, DEFAULT_SHUFFLE_MODE)
        set(value) {
            writePref(OPTIONS_SHUFFLE_MODE, value)
        }

    var optionsModeLoop: Boolean
        get() = prefs.getBoolean(OPTIONS_LOOP_MODE, DEFAULT_LOOP_MODE)
        set(value) {
            writePref(OPTIONS_LOOP_MODE, value)
        }
}
