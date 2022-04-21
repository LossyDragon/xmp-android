package org.helllabs.android.xmp.ui.preferences

import android.content.Intent
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import de.schnettler.datastore.compose.material3.PreferenceScreen
import de.schnettler.datastore.compose.material3.model.Preference
import de.schnettler.datastore.compose.material3.widget.PreferenceIcon
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.NavScreens
import org.helllabs.android.xmp.ui.components.XmpAppBar3
import org.helllabs.android.xmp.ui.preferences.about.About
import org.helllabs.android.xmp.ui.preferences.about.ListFormats
import org.helllabs.android.xmp.util.PrefManager2
import org.helllabs.android.xmp.util.PrefManager2.dataStore
import org.helllabs.android.xmp.util.launchActivity

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
    val context = LocalContext.current
    val dataStore = context.dataStore
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
            items = listOf(
                Preference.PreferenceGroup(
                    title = "Theme",
                    enabled = true,
                    preferenceItems = listOf(
                        Preference.PreferenceItem.ListPreference(
                            enabled = true,
                            entries = PrefManager2.prefThemeItems,
                            icon = { PreferenceIcon(icon = Icons.Default.LightMode) },
                            request = PrefManager2.themeRequest,
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
                                navController.navigate(NavScreens.SettingsPlaylist.route)
                            },
                            singleLineTitle = true,
                            summary = "",
                            title = stringResource(R.string.pref_category_files),
                        ),
                        Preference.PreferenceItem.TextPreference(
                            enabled = true, // TODO disable if service is alive.
                            icon = { },
                            onClick = {
                                navController.navigate(NavScreens.SettingsSound.route)
                            },
                            singleLineTitle = true,
                            summary = "",
                            title = stringResource(R.string.pref_category_sound),
                        ),
                        Preference.PreferenceItem.TextPreference(
                            enabled = true,
                            icon = { },
                            onClick = {
                                navController.navigate(NavScreens.SettingsInterface.route)
                            },
                            singleLineTitle = true,
                            summary = "",
                            title = stringResource(R.string.pref_category_interface),
                        ),
                        Preference.PreferenceItem.TextPreference(
                            enabled = true,
                            icon = { },
                            onClick = {
                                navController.navigate(NavScreens.SettingsDownload.route)
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
                                context.launchActivity(Intent(context, ListFormats::class.java))
                            },
                            singleLineTitle = true,
                            summary = stringResource(R.string.pref_list_formats_summary),
                            title = stringResource(R.string.pref_list_formats_title),
                        ),
                        Preference.PreferenceItem.TextPreference(
                            enabled = true,
                            icon = { PreferenceIcon(icon = Icons.Default.Info) },
                            onClick = {
                                context.launchActivity(Intent(context, About::class.java))
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
            dataStore = dataStore,
            statusBarPadding = true
        )
    }
}
