package org.helllabs.android.xmp.compose.ui.search.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import java.util.Locale
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.search.components.GuruFrame

@Composable
fun SearchErrorScreen(
    modifier: Modifier,
    message: String?,
    onBack: () -> Unit
) {
    val errorMsg = remember {
        message?.substringAfter("Exception: ")?.trim()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            XmpTopBar(
                onBack = onBack,
                title = stringResource(id = R.string.screen_title_error)
            )
        }
    ) { paddingValues ->
        val configuration = LocalConfiguration.current
        val modifier = remember(configuration.orientation) {
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                Modifier
            } else {
                Modifier.displayCutoutPadding()
            }
        }

        GuruFrame(
            modifier = modifier.padding(paddingValues),
            message = when {
                errorMsg.isNullOrEmpty() -> stringResource(R.string.search_unknown_error)

                else -> stringResource(
                    R.string.search_known_error,
                    errorMsg.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
                )
            }
        )
    }
}

private class ErrorMessageParameterProvider : PreviewParameterProvider<String?> {
    override val values: Sequence<String?> = sequenceOf(
        null,
        "Exception: Some Error Message"
    )
}

@Preview
@Composable
private fun Preview(
    @PreviewParameter(ErrorMessageParameterProvider::class) message: String?
) {
    XmpTheme(useDarkTheme = true) {
        SearchErrorScreen(
            modifier = Modifier,
            message = message,
            onBack = {}
        )
    }
}
