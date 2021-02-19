package org.helllabs.android.xmp.preferences

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.preference.Preference
import android.preference.PreferenceScreen
import com.fnp.materialpreferences.PreferenceActivity
import com.fnp.materialpreferences.PreferenceFragment
import java.io.File
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.Message.toast

class Preferences : PreferenceActivity() {

    // private String oldPath;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
          We load a PreferenceFragment which is the recommended way by Android
          see @http://developer.android.com/guide/topics/ui/settings.html#Fragment
          @TargetApi(11)
         */
        setPreferenceFragment(MyPreferenceFragment())
    }

    // @Override
    // public boolean onKeyDown(final int keyCode, final KeyEvent event) {
    // 	if (event.getAction() == KeyEvent.ACTION_DOWN) {
    // 		if (keyCode == KeyEvent.KEYCODE_BACK) {
    // 			final String newPath = prefs.getString(MEDIA_PATH, DEFAULT_MEDIA_PATH);
    // 			setResult(newPath.equals(oldPath) ? RESULT_CANCELED : RESULT_OK);
    // 			finish();
    // 		}
    // 	}
    //
    // 	return super.onKeyDown(keyCode, event);
    // }

    class MyPreferenceFragment : PreferenceFragment() {
        // private SharedPreferences prefs;
        private var ctx: Context? = null
        override fun addPreferencesFromResource(): Int {
            return R.xml.preferences
        }

        override fun onAttach(activity: Activity) {
            super.onAttach(activity)
            ctx = activity.baseContext
            // prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // setTheme(R.style.PreferencesTheme);

            // oldPath = prefs.getString(MEDIA_PATH, DEFAULT_MEDIA_PATH);
            // addPreferencesFromResource(R.xml.preferences);
            val soundScreen = findPreference("sound_screen") as PreferenceScreen
            soundScreen.isEnabled = !PlayerService.isAlive
            val clearCache = findPreference("clear_cache")
            clearCache.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (deleteCache(CACHE_DIR)) {
                    toast(ctx, getString(R.string.cache_clear))
                } else {
                    toast(ctx, getString(R.string.cache_clear_error))
                }
                true
            }
        }

        companion object {
            fun deleteCache(file: File): Boolean {
                return deleteCache(file, true)
            }

            private fun deleteCache(file: File, flag: Boolean): Boolean {
                var flag = flag
                if (!file.exists()) {
                    return true
                }
                if (file.isDirectory) {
                    for (cacheFile in file.listFiles()) {
                        flag = flag and deleteCache(cacheFile)
                    }
                }
                flag = flag and file.delete()
                return flag
            }
        }
    }

    companion object {
        val SD_DIR = Environment.getExternalStorageDirectory()
        val DATA_DIR = File(SD_DIR, "Xmp for Android")
        val CACHE_DIR = File(SD_DIR, "Android/data/org.helllabs.android.xmp/cache/")
        val DEFAULT_MEDIA_PATH = "$SD_DIR/mod"

        const val MEDIA_PATH = "media_path"
        const val VOL_BOOST = "vol_boost"
        const val CHANGELOG_VERSION = "changelog_version"

        // public static final String STEREO = "stereo";
        // public static final String PAN_SEPARATION = "pan_separation";
        // change the variable name so we can use the new default mix value
        const val STEREO_MIX = "stereo_mix"
        const val DEFAULT_PAN = "default_pan"
        const val PLAYLIST_MODE = "playlist_mode"
        const val AMIGA_MIXER = "amiga_mixer"

        // Don't use PREF_INTERPOLATION -- was boolean in 2.x and string in 3.2.0
        const val INTERPOLATE = "interpolate"
        const val INTERP_TYPE = "interp_type"

        // public static final String FILTER = "filter";
        const val EXAMPLES = "examples"
        const val SAMPLING_RATE = "sampling_rate"

        // public static final String BUFFER_MS = "buffer_ms";
        const val BUFFER_MS = "buffer_ms_opensl"
        const val SHOW_TOAST = "show_toast"
        const val SHOW_INFO_LINE = "show_info_line"
        const val USE_FILENAME = "use_filename"

        // public static final String TITLES_IN_BROWSER = "titles_in_browser";
        const val ENABLE_DELETE = "enable_delete"
        const val KEEP_SCREEN_ON = "keep_screen_on"
        const val HEADSET_PAUSE = "headset_pause"
        const val ALL_SEQUENCES = "all_sequences"

        // public static final String BACK_BUTTON_PARENTDIR = "back_button_parentdir";
        const val BACK_BUTTON_NAVIGATION = "back_button_navigation"
        const val BLUETOOTH_PAUSE = "bluetooth_pause"
        const val START_ON_PLAYER = "start_on_player"
        const val MODARCHIVE_FOLDER = "modarchive_folder"
        const val ARTIST_FOLDER = "artist_folder"
    }
}
