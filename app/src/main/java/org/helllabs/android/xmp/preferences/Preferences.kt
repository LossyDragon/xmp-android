package org.helllabs.android.xmp.preferences

import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.preferences.Preferences.Companion.CACHE_DIR
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.Message
import java.io.File


class Preferences : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.pref_layout)

        supportActionBar?.run {
            setDisplayShowHomeEnabled(true)
            //setDisplayHomeAsUpEnabled(true)
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
    }

    companion object {
        private val SD_DIR: File = Environment.getExternalStorageDirectory()
        val DATA_DIR = File(SD_DIR, "Xmp for Android")
        val CACHE_DIR = File(SD_DIR, "Android/data/org.helllabs.android.xmp/cache/")

        val DEFAULT_MEDIA_PATH = "$SD_DIR/mod"
        const val ALL_SEQUENCES = "all_sequences"
        const val AMIGA_MIXER = "amiga_mixer"
        //const val APP_THEME = "themePref"
        const val ARTIST_FOLDER = "artist_folder"
        const val BACK_BUTTON_NAVIGATION = "back_button_navigation"
        const val BLUETOOTH_PAUSE = "bluetooth_pause"
        const val BUFFER_MS = "buffer_ms_opensl"
        const val CHANGELOG_VERSION = "changelog_version"
        const val DEFAULT_PAN = "default_pan"
        const val ENABLE_DELETE = "enable_delete"
        const val EXAMPLES = "examples"
        const val HEADSET_PAUSE = "headset_pause"
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

        //public static final String STEREO = "stereo";
        //public static final String PAN_SEPARATION = "pan_separation";
        //public static final String BACK_BUTTON_PARENTDIR = "back_button_parentdir";
        //public static final String TITLES_IN_BROWSER = "titles_in_browser";
        //public static final String BUFFER_MS = "buffer_ms";
        //public static final String FILTER = "filter";
    }
}

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val soundScreen: PreferenceScreen? = findPreference("sound_screen")
        soundScreen?.isEnabled = !PlayerService.isAlive

        val clearCache: Preference? = findPreference("clear_cache")
        clearCache?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (deleteCache(CACHE_DIR)) {
                Message.toast(context!!, getString(R.string.cache_clear))
            } else {
                Message.toast(context!!, getString(R.string.cache_clear_error))
            }
            true
        }

        //TODO Day Night Theme
//        val appTheme: ListPreference? = findPreference("themePref")
//        appTheme?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
//            setTheme(newValue.toString())
//            true
//        }
    }

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        if (arguments != null) {
            setPreferencesFromResource(R.xml.preferences, arguments!!.getString("rootKey"))
        } else {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }
    }

    override fun onNavigateToScreen(preferenceScreen: PreferenceScreen) {
        val applicationPreferencesFragment = SettingsFragment()
        val args = Bundle()
        args.putString("rootKey", preferenceScreen.key)
        applicationPreferencesFragment.arguments = args
        fragmentManager!!
                .beginTransaction()
                .replace(id, applicationPreferencesFragment)
                .addToBackStack(null)
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (fragmentManager!!.backStackEntryCount > 0) {
                    fragmentManager?.popBackStack()
                } else {
                    activity?.onBackPressed()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        //TODO Day Night theme
//        fun setTheme(theme: String) {
//            XmpApplication.applyTheme(theme)
//        }

        fun deleteCache(file: File): Boolean {
            return deleteCache(file, true)
        }

        @Suppress("SameParameterValue")
        private fun deleteCache(file: File, flag: Boolean): Boolean {
            var booleanFlag = flag
            if (!file.exists()) {
                return true
            }

            if (file.isDirectory) {
                for (cacheFile in file.listFiles()!!) {
                    booleanFlag = booleanFlag and deleteCache(cacheFile)
                }
            }
            booleanFlag = booleanFlag and file.delete()

            return booleanFlag

        }
    }

}


