package org.helllabs.android.xmp.preferences;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.service.PlayerService;
import org.helllabs.android.xmp.util.Message;

import java.io.File;

public class Preferences extends com.fnp.materialpreferences.PreferenceActivity {
    public static final File SD_DIR = Environment.getExternalStorageDirectory();
    public static final File DATA_DIR = new File(SD_DIR, "Xmp for Android");
    public static final File CACHE_DIR = new File(SD_DIR, "Android/data/org.helllabs.android.xmp/cache/");

    public static final String DEFAULT_MEDIA_PATH = SD_DIR.toString() + "/mod";
    public static final String MEDIA_PATH = "media_path";
    public static final String VOL_BOOST = "vol_boost";
    public static final String CHANGELOG_VERSION = "changelog_version";
    //public static final String STEREO = "stereo";
    //public static final String PAN_SEPARATION = "pan_separation";
    // change the variable name so we can use the new default mix value
    public static final String STEREO_MIX = "stereo_mix";
    public static final String DEFAULT_PAN = "default_pan";
    public static final String PLAYLIST_MODE = "playlist_mode";
    public static final String AMIGA_MIXER = "amiga_mixer";
    // Don't use PREF_INTERPOLATION -- was boolean in 2.x and string in 3.2.0
    public static final String INTERPOLATE = "interpolate";
    public static final String INTERP_TYPE = "interp_type";
    //public static final String FILTER = "filter";
    public static final String EXAMPLES = "examples";
    public static final String SAMPLING_RATE = "sampling_rate";
    //public static final String BUFFER_MS = "buffer_ms";
    public static final String BUFFER_MS = "buffer_ms_opensl";
    public static final String SHOW_TOAST = "show_toast";
    public static final String SHOW_INFO_LINE = "show_info_line";
    public static final String USE_FILENAME = "use_filename";
    //public static final String TITLES_IN_BROWSER = "titles_in_browser";
    public static final String ENABLE_DELETE = "enable_delete";
    public static final String KEEP_SCREEN_ON = "keep_screen_on";
    public static final String HEADSET_PAUSE = "headset_pause";
    public static final String ALL_SEQUENCES = "all_sequences";
    //public static final String BACK_BUTTON_PARENTDIR = "back_button_parentdir";
    public static final String BACK_BUTTON_NAVIGATION = "back_button_navigation";
    public static final String BLUETOOTH_PAUSE = "bluetooth_pause";
    public static final String START_ON_PLAYER = "start_on_player";
    public static final String MODARCHIVE_FOLDER = "modarchive_folder";
    public static final String ARTIST_FOLDER = "artist_folder";


    //private String oldPath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
          We load a PreferenceFragment which is the recommended way by Android
          see @http://developer.android.com/guide/topics/ui/settings.html#Fragment
          @TargetApi(11)
         */
        setPreferenceFragment(new MyPreferenceFragment());
    }

    public static class MyPreferenceFragment extends com.fnp.materialpreferences.PreferenceFragment {
        //private SharedPreferences prefs;
        private Context context;

        @Override
        public int addPreferencesFromResource() {
            return R.xml.preferences;
        }

        @Override
        public void onAttach(final Activity activity) {
            super.onAttach(activity);
            context = activity.getBaseContext();
            //prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            //setTheme(R.style.PreferencesTheme);
            super.onCreate(savedInstanceState);

            //oldPath = prefs.getString(MEDIA_PATH, DEFAULT_MEDIA_PATH);
            //addPreferencesFromResource(R.xml.preferences);

            final PreferenceScreen soundScreen = (PreferenceScreen) findPreference("sound_screen");
            soundScreen.setEnabled(!PlayerService.isAlive);

            final Preference clearCache = findPreference("clear_cache");
            clearCache.setOnPreferenceClickListener(preference -> {
                if (deleteCache(CACHE_DIR)) {
                    Message.toast(context, getString(R.string.cache_clear));
                } else {
                    Message.toast(context, getString(R.string.cache_clear_error));
                }
                return true;
            });
        }

        public static boolean deleteCache(final File file) {
            return deleteCache(file, true);
        }

        private static boolean deleteCache(final File file, boolean flag) {
            if (!file.exists()) {
                return true;
            }

            if (file.isDirectory()) {
                for (final File cacheFile : file.listFiles()) {
                    flag &= deleteCache(cacheFile);
                }
            }
            flag &= file.delete();

            return flag;
        }
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
