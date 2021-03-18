package org.helllabs.android.xmp.ui.preferences

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.*
import java.io.File
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.toast

class PreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Observe live if player is alive
        PlayerService.isPlayerAlive.observe(
            this,
            { isAlive ->
                findPreference<PreferenceScreen>("sound_screen")?.let {
                    if (isAlive) {
                        it.isEnabled = false
                        it.title = getString(R.string.pref_category_sound_disabled)
                    } else {
                        it.isEnabled = true
                        it.title = getString(R.string.pref_category_sound)
                    }
                }
            }
        )

        findPreference<Preference>("clear_cache")?.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (deleteCache(activity?.externalCacheDir!!)) {
                    activity?.toast(R.string.cache_clear)
                } else {
                    activity?.toast(R.string.cache_clear_error)
                }
                true
            }
        }

        // It kinda works.
        findPreference<SeekBarPreference>("buffer_ms_opensl")?.let {
            it.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    // Round to the nearest multiple of 10
                    var value = newValue as Int
                    val mod = value % 10

                    value = if (mod > 5) {
                        value + (10 - mod)
                    } else {
                        value - mod
                    }

                    it.value = value
                    false
                }
        }
    }

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        logD("onCreatePreferences: ${arguments?.getString("rootKey")} or $rootKey")
        if (arguments != null) {
            setPreferencesFromResource(R.xml.preferences, arguments?.getString("rootKey"))
        } else {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }
    }

    override fun onNavigateToScreen(preferenceScreen: PreferenceScreen) {
        val applicationPreferencesFragment = PreferencesFragment()
        applicationPreferencesFragment.arguments = Bundle().apply {
            putString("rootKey", preferenceScreen.key)
        }
        parentFragmentManager
            .beginTransaction()
            .replace(id, applicationPreferencesFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                activity?.onBackPressed()
            }
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun deleteCache(file: File): Boolean = deleteCache(file, true)

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
