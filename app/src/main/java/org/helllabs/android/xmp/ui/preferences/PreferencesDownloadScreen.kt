package org.helllabs.android.xmp.ui.preferences

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import de.schnettler.datastore.compose.material3.PreferenceScreen
import de.schnettler.datastore.compose.material3.model.Preference
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.NavScreens
import org.helllabs.android.xmp.ui.components.XmpAppBar3
import org.helllabs.android.xmp.util.PrefManager2
import org.helllabs.android.xmp.util.PrefManager2.dataStore

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun PreferencesDownloadScreen(
    navController: NavController,
    onBackPressedCallback: OnBackPressedDispatcher
) {
    val context = LocalContext.current
    val dataStore = context.dataStore

    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    val onBackPressed = {
        navController.popBackStack(route = NavScreens.Settings.route, inclusive = false)
    }

    val callback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        }
    }

    DisposableEffect(onBackPressedCallback) {
        onBackPressedCallback.addCallback(callback)
        onDispose {
            callback.remove()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            XmpAppBar3(
                title = { Text(text = stringResource(id = R.string.pref_category_modarchive)) },
                scrollBehavior = scrollBehavior,
                onNavIconPressed = { onBackPressed() }

            )
        },
    ) { innerPadding ->
        PreferenceScreen(
            items = listOf(
                Preference.PreferenceGroup(
                    title = stringResource(id = R.string.pref_category_download),
                    enabled = true,
                    preferenceItems = listOf(
                        Preference.PreferenceItem.SwitchPreference(
                            icon = {},
                            request = PrefManager2.useTmaFolderRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_modarchive_folder_summary),
                            title = stringResource(id = R.string.pref_modarchive_folder_title),
                        ),
                        Preference.PreferenceItem.SwitchPreference(
                            icon = {},
                            request = PrefManager2.useArtistFolderRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_artist_folder_summary),
                            title = stringResource(id = R.string.pref_artist_folder_title),
                        )
                    )
                )
            ),
            dataStore = dataStore,
            contentPadding = innerPadding,
        )
    }
}
