package org.helllabs.android.xmp.util.preferences

import androidx.datastore.preferences.core.stringPreferencesKey
import de.schnettler.datastore.manager.PreferenceRequest

/**
 * Start directory for explorer
 */
val requestMediaPath = PreferenceRequest(
    key = stringPreferencesKey("pref_media_path"),
    defaultValue = "",
)
