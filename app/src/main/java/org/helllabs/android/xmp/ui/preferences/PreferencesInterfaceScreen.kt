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
import com.google.accompanist.insets.navigationBarsPadding
import de.schnettler.datastore.compose.material3.PreferenceScreen
import de.schnettler.datastore.compose.material3.model.Preference
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.NavScreens
import org.helllabs.android.xmp.ui.components.XmpAppBar3
import org.helllabs.android.xmp.util.PrefManager
import org.helllabs.android.xmp.util.PrefManager.dataStore

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun PreferencesInterfaceScreen(
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
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        topBar = {
            XmpAppBar3(
                title = { Text(text = stringResource(id = R.string.pref_category_interface)) },
                scrollBehavior = scrollBehavior,
                onNavIconPressed = { onBackPressed() }

            )
        },
    ) { innerPadding ->
        PreferenceScreen(
            contentPadding = innerPadding,
            dataStore = dataStore,
            items = listOf(
                Preference.PreferenceGroup(
                    enabled = true,
                    title = stringResource(id = R.string.pref_category_information),
                    preferenceItems = listOf(
                        Preference.PreferenceItem.SwitchPreference(
                            enabled = true,
                            icon = {},
                            request = PrefManager.replayInfoRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_show_info_line_summary),
                            title = stringResource(id = R.string.pref_show_info_line_title),
                        ),
                    ),
                ),
                Preference.PreferenceGroup(
                    enabled = true,
                    title = stringResource(id = R.string.pref_category_iface_screen),
                    preferenceItems = listOf(
                        Preference.PreferenceItem.SwitchPreference(
                            enabled = true,
                            icon = {},
                            request = PrefManager.keepScreenOnRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_keep_screen_on_summary),
                            title = stringResource(id = R.string.pref_keep_screen_on_title),
                        ),
                        Preference.PreferenceItem.SwitchPreference(
                            enabled = true,
                            icon = {},
                            request = PrefManager.launchInPlayerRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_start_on_player_summary),
                            title = stringResource(id = R.string.pref_start_on_player_title),
                        )
                    ),
                ),
                Preference.PreferenceGroup(
                    enabled = true,
                    title = stringResource(id = R.string.pref_category_player_interface),
                    preferenceItems = listOf(
                        Preference.PreferenceItem.SwitchPreference(
                            enabled = true,
                            icon = {},
                            request = PrefManager.showHexValuesRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_player_hex_summary),
                            title = stringResource(id = R.string.pref_player_hex_title),
                        ),
                        Preference.PreferenceItem.SwitchPreference(
                            enabled = true,
                            icon = {},
                            request = PrefManager.useBetterWaveformRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_draw_lines_summary),
                            title = stringResource(id = R.string.pref_draw_lines_title),
                        )
                    ),
                ),
                Preference.PreferenceGroup(
                    enabled = true,
                    title = stringResource(id = R.string.pref_category_notifications),
                    preferenceItems = listOf(
                        Preference.PreferenceItem.SwitchPreference(
                            enabled = true,
                            icon = {},
                            request = PrefManager.useMediaStyleNotificationRequest,
                            singleLineTitle = true,
                            summary = stringResource(id = R.string.pref_notification_summary),
                            title = stringResource(id = R.string.pref_notification_title),
                        ),
                    ),
                ),
            ),
        )
    }
}
