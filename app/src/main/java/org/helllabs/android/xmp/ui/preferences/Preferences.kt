package org.helllabs.android.xmp.ui.preferences

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.components.XmpAppBar3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    navController: NavController,
    onBackPressedCallback: OnBackPressedDispatcher
) {
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    val activity = LocalContext.current as AppCompatActivity
    val supportFragmentManager = activity.supportFragmentManager

    val callback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val stackCount = supportFragmentManager.backStackEntryCount
                if (stackCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    this.isEnabled = false
                    navController.popBackStack()
                }
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
        topBar = {
            XmpAppBar3(
                scrollBehavior = scrollBehavior,
                onNavIconPressed = { navController.popBackStack() },
                titleText = stringResource(id = R.string.pref_category_preferences),
            )
        }
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            factory = { context ->
                FrameLayout(context).apply {
                    id = R.id.composeFrameLayout
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }.also {
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.composeFrameLayout, PreferencesFragment())
                        .commit()
                }
            }
        )
    }
}
