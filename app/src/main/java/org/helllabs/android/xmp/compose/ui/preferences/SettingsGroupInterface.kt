package org.helllabs.android.xmp.compose.ui.preferences

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsSwitch
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.core.PrefManager
import org.koin.compose.koinInject

@Composable
fun SettingsGroupInterface() {
    val prefManager: PrefManager = koinInject()
    val scope = rememberCoroutineScope()

    // Collect preferences as state
    val showInfoLineValue by prefManager.showInfoLineFlow().collectAsStateWithLifecycle(
        initialValue = true
    )
    val keepScreenOnValue by prefManager.keepScreenOnFlow().collectAsStateWithLifecycle(
        initialValue = false
    )
    val showHexValue by prefManager.showHexFlow().collectAsStateWithLifecycle(initialValue = false)

    SettingsGroup(
        title = { Text(text = stringResource(id = R.string.pref_category_interface)) }
    ) {
        // Show Info Line
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_show_info_line_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_show_info_line_summary)) },
            state = showInfoLineValue,
            onCheckedChange = {
                scope.launch {
                    prefManager.setShowInfoLine(it)
                }
            }
        )

        // Keep Screen On
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_keep_screen_on_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_keep_screen_on_summary)) },
            state = keepScreenOnValue,
            onCheckedChange = {
                scope.launch {
                    prefManager.setKeepScreenOn(it)
                }
            }
        )

        // Show Hex
        SettingsSwitch(
            title = { Text(text = "Show hex values") },
            subtitle = { Text(text = "Show hex values for player info") },
            state = showHexValue,
            onCheckedChange = {
                scope.launch {
                    prefManager.setShowHex(it)
                }
            }
        )
    }
}
