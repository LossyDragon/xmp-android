package org.helllabs.android.xmp.ui.search

import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.ProvideWindowInsets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.components.XmpAppBar3
import org.helllabs.android.xmp.ui.search.ModArchiveConstants.ERROR
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.ui.theme.topazFontFamily
import org.helllabs.android.xmp.util.upperCase

class SearchError : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Extract the error message
        var message: String? = intent.getStringExtra(ERROR)
        if (message == null) {
            message = getString(R.string.search_unknown_error)
        } else {
            // Remove java exception stuff
            val idx = message.indexOf("Exception: ")
            if (idx >= 0) {
                message = message.substring(idx + 11)
            }
            message = if (message.trim().isEmpty()) {
                getString(R.string.search_unknown_error)
            } else {
                val err = message.substring(0, 1).upperCase() + message.substring(1)
                getString(R.string.search_known_error, err)
            }
        }

        setContent {
            ProvideWindowInsets {
                ErrorLayout(message = message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorLayout(
    message: String,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val backCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(context, Search::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                context.startActivity(intent)
            }
        }
    }

    SideEffect { backCallback.isEnabled = true }
    DisposableEffect(lifecycleOwner, backDispatcher) {
        backDispatcher?.addCallback(lifecycleOwner, backCallback)
        onDispose { backCallback.remove() }
    }

    XmpTheme3 {
        Scaffold(
            topBar = {
                XmpAppBar3(
                    onNavIconPressed = {
                        backCallback.handleOnBackPressed()
                    },
                    titleText = stringResource(id = R.string.search_title_error)
                )
            }
        ) {
            GuruFrame(message)
        }
    }
}

@Composable
private fun GuruFrame(message: String) {
    val scope = rememberCoroutineScope()
    var frameState by remember { mutableStateOf(true) }

    SideEffect {
        scope.launch {
            delay(1337L)
            frameState = !frameState
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(5.dp, if (frameState) Color.Red else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier.padding(12.dp),
            text = message,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            fontFamily = topazFontFamily,
            fontSize = 16.sp,
            color = Color.Red
        )
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Light Theme", uiMode = UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun ErrorLayoutPreview() {
    ErrorLayout(
        message = "Guru Error\nGuru Error",
    )
}
