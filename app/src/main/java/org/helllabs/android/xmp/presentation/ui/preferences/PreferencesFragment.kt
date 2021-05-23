package org.helllabs.android.xmp.presentation.ui.preferences

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.*
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.logD

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

        findPreference<Preference>("reset_media_path")?.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@PreferencesFragment)
                    title(R.string.dialog_reset_media_path_title)
                    message(R.string.dialog_reset_media_path_message)
                    positiveButton(R.string.ok) {
                        PrefManager.removePref("media_path")
                        logD("Media path is now ${PrefManager.mediaPath}")
                    }
                    negativeButton(R.string.cancel)
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
}
