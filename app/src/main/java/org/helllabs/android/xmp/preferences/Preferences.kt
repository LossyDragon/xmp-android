package org.helllabs.android.xmp.preferences

import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.util.fatalError
import org.helllabs.android.xmp.util.isAtMostN
import java.io.File

class Preferences : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.pref_layout)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, PreferencesFragment())
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {

            // Known issue with API <= 24 for themes
            // https://android-review.googlesource.com/c/platform/frameworks/support/+/971248
            // https://issuetracker.google.com/issues/131851825
            if (isAtMostN() && XmpApplication.instance!!.isThemeChanged) {
                fatalError(R.string.change_theme_older_apis)
            } else {
                onBackPressed()
            }
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        // SAF absolutely sucks. Keep until that `pile` is better documented.
        @Suppress("DEPRECATION")
        private val SD_DIR: File = Environment.getExternalStorageDirectory()
        val DATA_DIR: File = File(SD_DIR, "Xmp for Android")
        val CACHE_DIR: File = File(SD_DIR, "Android/data/org.helllabs.android.xmp/cache/")

        val DEFAULT_MEDIA_PATH = "$SD_DIR/mod"
        const val ALL_SEQUENCES = "all_sequences"
        const val AMIGA_MIXER = "amiga_mixer"
        const val APP_THEME = "themePref"
        const val ARTIST_FOLDER = "artist_folder"
        const val BACK_BUTTON_NAVIGATION = "back_button_navigation"
        // const val BLUETOOTH_PAUSE = "bluetooth_pause"
        const val BUFFER_MS = "buffer_ms_opensl"
        const val CHANGELOG_VERSION = "changelog_version"
        const val DEFAULT_PAN = "default_pan"
        const val ENABLE_DELETE = "enable_delete"
        const val EXAMPLES = "examples"
        // const val HEADSET_PAUSE = "headset_pause"
        const val INTERPOLATE = "interpolate"
        const val INTERP_TYPE = "interp_type"
        const val KEEP_SCREEN_ON = "keep_screen_on"
        const val MEDIA_PATH = "media_path"
        const val MODARCHIVE_FOLDER = "modarchive_folder"
        const val PLAYLIST_MODE = "playlist_mode"
        const val SAMPLING_RATE = "sampling_rate"
        const val SHOW_INFO_LINE = "show_info_line"
        const val SHOW_TOAST = "show_toast"
        const val START_ON_PLAYER = "start_on_player"
        const val STEREO_MIX = "stereo_mix"
        const val USE_FILENAME = "use_filename"
        const val VOL_BOOST = "vol_boost"
        const val PLAYER_DRAG_LOCK = "player_drag_lock"
        const val PLAYER_DRAG_STAY = "player_drag_stay"
        const val OPTIONS_SHUFFLE_MODE = "options_shuffleMode"
        const val OPTIONS_LOOP_MODE = "options_loopMode"

        // public static final String STEREO = "stereo";
        // public static final String PAN_SEPARATION = "pan_separation";
        // public static final String BACK_BUTTON_PARENTDIR = "back_button_parentdir";
        // public static final String TITLES_IN_BROWSER = "titles_in_browser";
        // public static final String BUFFER_MS = "buffer_ms";
        // public static final String FILTER = "filter";
    }
}
