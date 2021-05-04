package org.helllabs.android.xmp.presentation.ui.search

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.presentation.components.AppBar
import org.helllabs.android.xmp.presentation.theme.*
import org.helllabs.android.xmp.presentation.ui.search.ModArchiveConstants.ERROR
import org.helllabs.android.xmp.util.upperCase

class SearchError : ComponentActivity() {

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
            message = if (message.trim { it <= ' ' }.isEmpty()) {
                getString(R.string.search_unknown_error)
            } else {
                val err = message.substring(0, 1).upperCase() + message.substring(1)
                getString(R.string.search_known_error, err)
            }
        }

        setContent {
            ErrorLayout(
                appTitle = stringResource(id = R.string.search_title_error),
                onBack = { onBackPressed() },
                message = message,
            )
        }
    }

    // For some reason it ignores the manifest config
    override fun onBackPressed() {
        super.onBackPressed()
        // Return to the Search
        Intent(this, Search::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }.also {
            startActivity(it)
        }
    }
}

@Composable
private fun ErrorLayout(
    isDarkTheme: Boolean = systemDarkTheme(),
    appTitle: String,
    onBack: () -> Unit,
    message: String,
) {
    AppTheme(
        isDarkTheme = isDarkTheme
    ) {
        Scaffold(
            topBar = {
                AppBar(
                    title = appTitle,
                    navIconClick = { onBack() },
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

@Preview
@Composable
private fun ErrorLayoutPreview() {
    ErrorLayout(
        isDarkTheme = false,
        appTitle = stringResource(id = R.string.search_title_error),
        onBack = {},
        message = "Guru Error\nGuru Error",
    )
}

@Preview
@Composable
private fun ErrorLayoutPreviewDark() {
    ErrorLayout(
        isDarkTheme = true,
        appTitle = stringResource(id = R.string.search_title_error),
        onBack = {},
        message = "Guru Error\nGuru Error",
    )
}
