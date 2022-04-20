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
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.NavScreens
import org.helllabs.android.xmp.ui.components.XmpAppBar3
import org.helllabs.android.xmp.util.PrefManager2.dataStore

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
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
        modifier = Modifier.fillMaxSize(),
        topBar = {
            XmpAppBar3(
                title = { Text(text = stringResource(id = R.string.pref_category_interface)) },
                scrollBehavior = scrollBehavior,
                onNavIconPressed = { onBackPressed() }

            )
        },
    ) { innerPadding ->
        PreferenceScreen(
            items = listOf(),
            dataStore = dataStore,
            contentPadding = innerPadding,
        )
    }
}
