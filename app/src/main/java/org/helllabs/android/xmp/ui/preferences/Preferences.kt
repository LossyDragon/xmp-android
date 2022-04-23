package org.helllabs.android.xmp.ui.preferences

import androidx.activity.OnBackPressedDispatcher
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import de.schnettler.datastore.compose.material3.PreferenceScreen
import de.schnettler.datastore.compose.material3.model.Preference
import de.schnettler.datastore.compose.material3.widget.PreferenceIcon
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.NavScreensSettings
import org.helllabs.android.xmp.ui.components.XmpAppBar3
import org.helllabs.android.xmp.util.PrefManager

private const val supportUrl = "https://github.com/cmatsuoka/xmp-android/issues"
private const val repoUrl = "https://github.com/libxmp/libxmp"
private const val libXmpUrl = "http://xmp.sourceforge.net/"

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun PreferencesScreen(
    navController: NavController,
    onBackPressedCallback: OnBackPressedDispatcher
) {
    val uriHandler = LocalUriHandler.current

    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    Scaffold(
        topBar = {
            XmpAppBar3(
                scrollBehavior = scrollBehavior,
                onNavIconPressed = { navController.popBackStack() },
                titleText = stringResource(id = R.string.pref_category_preferences),
            )
        }
    ) {
        PreferenceScreen(
            dataStore = PrefManager.dataStoreManager.dataStore,
            statusBarPadding = true,
            items = listOf(
                Preference.PreferenceGroup(
                    title = "Theme",
                    enabled = true,
                    preferenceItems = listOf(
                        Preference.PreferenceItem.ListPreference(
                            enabled = true,
                            entries = PrefManager.prefThemeItems,
                            icon = { PreferenceIcon(icon = Icons.Default.LightMode) },
                            request = PrefManager.themeRequest,
                            singleLineTitle = true,
                            summary = stringResource(R.string.pref_summary_theme),
                            title = stringResource(R.string.pref_title_theme),
                        )
                    )
                ),
                Preference.PreferenceGroup(
                    stringResource(id = R.string.pref_category_preferences),
                    enabled = true,
                    preferenceItems = listOf(
                        Preference.PreferenceItem.TextPreference(
                            enabled = true,
                            icon = { },
                            onClick = {
                                navController.navigate(NavScreensSettings.Playlist.route)
                            },
                            singleLineTitle = true,
                            summary = "",
                            title = stringResource(R.string.pref_category_files),
                        ),
                        Preference.PreferenceItem.TextPreference(
                            enabled = true, // TODO disable if service is alive.
                            icon = { },
                            onClick = {
                                navController.navigate(NavScreensSettings.Sound.route)
                            },
                            singleLineTitle = true,
                            summary = "",
                            title = stringResource(R.string.pref_category_sound),
                        ),
                        Preference.PreferenceItem.TextPreference(
                            enabled = true,
                            icon = { },
                            onClick = {
                                navController.navigate(NavScreensSettings.Interface.route)
                            },
                            singleLineTitle = true,
                            summary = "",
                            title = stringResource(R.string.pref_category_interface),
                        ),
                        Preference.PreferenceItem.TextPreference(
                            enabled = true,
                            icon = { },
                            onClick = {
                                navController.navigate(NavScreensSettings.Download.route)
                            },
                            singleLineTitle = true,
                            summary = "",
                            title = stringResource(R.string.pref_category_download),
                        ),
                    )
                ),
                Preference.PreferenceGroup(
                    title = stringResource(R.string.pref_category_information),
                    enabled = true,
                    preferenceItems = listOf(
                        Preference.PreferenceItem.TextPreference(
                            enabled = true,
                            icon = { PreferenceIcon(icon = Icons.Default.MusicNote) },
                            onClick = {
                                navController.navigate(NavScreensSettings.Formats.route)
                            },
                            singleLineTitle = true,
                            summary = stringResource(R.string.pref_list_formats_summary),
                            title = stringResource(R.string.pref_list_formats_title),
                        ),
                        Preference.PreferenceItem.TextPreference(
                            enabled = true,
                            icon = { PreferenceIcon(icon = Icons.Default.Info) },
                            onClick = {
                                navController.navigate(NavScreensSettings.About.route)
                            },
                            singleLineTitle = true,
                            summary = stringResource(R.string.pref_about_summary),
                            title = stringResource(R.string.pref_about_title),
                        ),
                        Preference.PreferenceItem.TextPreference(
                            enabled = true,
                            icon = { PreferenceIcon(icon = Icons.Default.Description) },
                            onClick = {
                                uriHandler.openUri(repoUrl)
                            },
                            singleLineTitle = true,
                            summary = stringResource(R.string.pref_site_summary),
                            title = stringResource(R.string.pref_site_title),
                        ),
                        Preference.PreferenceItem.TextPreference(
                            enabled = true,
                            icon = { PreferenceIcon(icon = Icons.Default.Description) },
                            onClick = {
                                uriHandler.openUri(libXmpUrl)
                            },
                            singleLineTitle = true,
                            summary = stringResource(R.string.pref_lib_summary),
                            title = stringResource(R.string.pref_lib_title),
                        ),
                        Preference.PreferenceItem.TextPreference(
                            enabled = true,
                            icon = { PreferenceIcon(icon = Icons.Default.Help) },
                            onClick = {
                                uriHandler.openUri(supportUrl)
                            },
                            singleLineTitle = true,
                            summary = stringResource(R.string.pref_support_summary),
                            title = stringResource(R.string.pref_support_title),
                        ),
                    )
                )
            ),
        )
    }
}
