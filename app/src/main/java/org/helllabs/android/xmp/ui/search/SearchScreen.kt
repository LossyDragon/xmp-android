@file:OptIn(ExperimentalMaterial3Api::class)

package org.helllabs.android.xmp.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.helllabs.android.xmp.ui.components.TopAppBar
import org.helllabs.android.xmp.ui.destinations.PlaylistScreenDestination

@Destination
@Composable
fun SearchScreen(
    navigator: DestinationsNavigator
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Search")
                },
                onNavPressed = {
                    navigator.popBackStack(route = PlaylistScreenDestination, inclusive = false)
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
        ) {
            Text("Search")
        }
    }
}
