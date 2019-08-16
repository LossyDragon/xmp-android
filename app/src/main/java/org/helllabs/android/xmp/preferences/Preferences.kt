package org.helllabs.android.xmp.preferences

import android.app.Activity
import android.os.Bundle
import android.os.Environment
import android.preference.Preference.OnPreferenceClickListener
import android.preference.PreferenceScreen
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.Message
import java.io.File

class Preferences : com.fnp.materialpreferences.PreferenceActivity() {

    //private String oldPath;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * We load a PreferenceFragment which is the recommended way by Android
         * see @http://developer.android.com/guide/topics/ui/settings.html#Fragment
         * @TargetApi(11)
         */
        setPreferenceFragment(MyPreferenceFragment())
    }

    class MyPreferenceFragment : com.fnp.materialpreferences.PreferenceFragment() {
        //private SharedPreferences prefs;

        override fun addPreferencesFromResource(): Int {
            return R.xml.preferences
        }

        override fun onAttach(activity: Activity) {
            super.onAttach(activity)
            //prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            //setTheme(R.style.PreferencesTheme);
            super.onCreate(savedInstanceState)

            //oldPath = prefs.getString(MEDIA_PATH, DEFAULT_MEDIA_PATH);
            //addPreferencesFromResource(R.xml.preferences);

            val soundScreen = findPreference("sound_screen") as PreferenceScreen
            soundScreen.isEnabled = !PlayerService.isAlive

            val clearCache = findPreference("clear_cache")
            clearCache.onPreferenceClickListener = OnPreferenceClickListener {
                if (deleteCache(CACHE_DIR)) {
                    Message.toast(activity.applicationContext, getString(R.string.cache_clear))
                } else {
                    Message.toast(activity.applicationContext, getString(R.string.cache_clear_error))
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
                    for (cacheFile in file.listFiles()!!) {
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
        val MEDIA_PATH = "media_path"
        val VOL_BOOST = "vol_boost"
        val CHANGELOG_VERSION = "changelog_version"
        //public static final String STEREO = "stereo";
        //public static final String PAN_SEPARATION = "pan_separation";
        // change the variable name so we can use the new default mix value
        val STEREO_MIX = "stereo_mix"
        val DEFAULT_PAN = "default_pan"
        val PLAYLIST_MODE = "playlist_mode"
        val AMIGA_MIXER = "amiga_mixer"
        // Don't use PREF_INTERPOLATION -- was boolean in 2.x and string in 3.2.0
        val INTERPOLATE = "interpolate"
        val INTERP_TYPE = "interp_type"
        //public static final String FILTER = "filter";
        val EXAMPLES = "examples"
        val SAMPLING_RATE = "sampling_rate"
        //public static final String BUFFER_MS = "buffer_ms";
        val BUFFER_MS = "buffer_ms_opensl"
        val SHOW_TOAST = "show_toast"
        val SHOW_INFO_LINE = "show_info_line"
        val USE_FILENAME = "use_filename"
        //public static final String TITLES_IN_BROWSER = "titles_in_browser";
        val ENABLE_DELETE = "enable_delete"
        val KEEP_SCREEN_ON = "keep_screen_on"
        val HEADSET_PAUSE = "headset_pause"
        val ALL_SEQUENCES = "all_sequences"
        //public static final String BACK_BUTTON_PARENTDIR = "back_button_parentdir";
        val BACK_BUTTON_NAVIGATION = "back_button_navigation"
        val BLUETOOTH_PAUSE = "bluetooth_pause"
        val START_ON_PLAYER = "start_on_player"
        val MODARCHIVE_FOLDER = "modarchive_folder"
        val ARTIST_FOLDER = "artist_folder"
    }

    //@Override
    //public boolean onKeyDown(final int keyCode, final KeyEvent event) {
    //	if (event.getAction() == KeyEvent.ACTION_DOWN) {
    //		if (keyCode == KeyEvent.KEYCODE_BACK) {
    //			final String newPath = prefs.getString(MEDIA_PATH, DEFAULT_MEDIA_PATH);
    //			setResult(newPath.equals(oldPath) ? RESULT_CANCELED : RESULT_OK);
    //			finish();
    //		}
    //	}
    //
    //	return super.onKeyDown(keyCode, event);
    //}
}
