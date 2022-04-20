package org.helllabs.android.xmp.ui.preferences

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.*
import dagger.hilt.android.AndroidEntryPoint
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.logD

@AndroidEntryPoint
class PreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Observe live if player is alive
        PlayerService.isPlayerAlive.observe(
            this
        ) { isAlive ->
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

        findPreference<ListPreference>("themePref")?.let {
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                val value = newValue as String
                logD("Theme: $value")

                // no op

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
        val key = arguments?.getString("rootKey") ?: rootKey
        logD("onCreatePreferences: $key")
        setPreferencesFromResource(R.xml.preferences, key)
    }

    override fun onNavigateToScreen(preferenceScreen: PreferenceScreen) {
        val applicationPreferencesFragment = PreferencesFragment()
        applicationPreferencesFragment.arguments = Bundle().apply {
            putString("rootKey", preferenceScreen.key)
        }
        parentFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left_slow,
                R.anim.slide_out_right_slow
            )
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
}
