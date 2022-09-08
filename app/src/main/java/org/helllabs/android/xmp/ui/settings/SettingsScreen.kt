@file:OptIn(ExperimentalMaterial3Api::class)

package org.helllabs.android.xmp.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.helllabs.android.xmp.ui.components.TopAppBar
import org.helllabs.android.xmp.ui.destinations.PlaylistScreenDestination

@Destination
@Composable
fun SettingsScreen(
    navigator: DestinationsNavigator
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings")
                },
                onNavPressed = {
                    navigator.popBackStack(route = PlaylistScreenDestination, inclusive = false)
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("Settings")
        }
    }
}
