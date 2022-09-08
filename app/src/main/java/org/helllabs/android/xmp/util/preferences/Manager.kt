package org.helllabs.android.xmp.util.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import de.schnettler.datastore.manager.DataStoreManager

/* Pref Manager */
object Manager {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    lateinit var dataStoreManager: DataStoreManager

    fun init(context: Context) {
        dataStoreManager = DataStoreManager(context.dataStore)
    }
}
