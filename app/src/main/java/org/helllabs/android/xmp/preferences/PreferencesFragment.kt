package org.helllabs.android.xmp.preferences

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.toast
import java.io.File

class PreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val soundScreen: PreferenceScreen? = findPreference("sound_screen")
        if (PlayerService.isAlive) {
            soundScreen?.isEnabled = false
            soundScreen?.title = getString(R.string.pref_category_sound) + " (Disabled when playing)"
        }

        val clearCache: Preference? = findPreference("clear_cache")
        clearCache?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (deleteCache(Preferences.CACHE_DIR)) {
                activity!!.toast(R.string.cache_clear)
            } else {
                activity!!.toast(R.string.cache_clear_error)
            }
            true
        }

        val appTheme: ListPreference? = findPreference("themePref")
        appTheme?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            setTheme(newValue.toString())
            true
        }
    }

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        if (arguments != null) {
            setPreferencesFromResource(R.xml.preferences, arguments!!.getString("rootKey"))
        } else {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }
    }

    override fun onNavigateToScreen(preferenceScreen: PreferenceScreen) {
        val applicationPreferencesFragment = PreferencesFragment()
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

        fun setTheme(theme: String) {
            XmpApplication.applyTheme(theme)
        }

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
