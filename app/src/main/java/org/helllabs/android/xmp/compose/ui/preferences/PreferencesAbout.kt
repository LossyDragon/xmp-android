package org.helllabs.android.xmp.compose.ui.preferences

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import com.alorma.compose.settings.ui.SettingsMenuLink
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.components.themedText
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.michromaFontFamily

@Composable
fun AboutScreen(
    buildVersionName: String,
    buildVersionCode: Int,
    libVersion: String,
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()
    val isScrolled by remember {
        derivedStateOf {
            scrollState.value > 0
        }
    }

    Scaffold(
        topBar = {
            XmpTopBar(
                title = stringResource(id = R.string.screen_title_about),
                isScrolled = isScrolled,
                onBack = onBack
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

        Column(
            modifier = modifier
                .padding(
                    PaddingValues(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding(),
                        start = 16.dp,
                        end = 16.dp
                    )
                )
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = themedText(text = stringResource(id = R.string.app_name)),
                textAlign = TextAlign.Center,
                fontFamily = michromaFontFamily,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = michromaFontFamily,
                    baselineShift = BaselineShift(.3f)
                )
            )
            AboutText(
                string = stringResource(id = R.string.about_version, buildVersionName),
                bottomPadding = 0.dp,
                style = MaterialTheme.typography.titleMedium
            )
            AboutText(
                string = stringResource(id = R.string.about_version_code, buildVersionCode),
                topPadding = 0.dp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AboutText(
                string = stringResource(id = R.string.about_author),
                style = MaterialTheme.typography.bodyLarge
            )
            AboutText(
                string = stringResource(id = R.string.about_author_fork),
                style = MaterialTheme.typography.bodyLarge
            )
            AboutText(
                string = stringResource(id = R.string.about_xmp, libVersion),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(.85f),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                text = "Links",
                fontFamily = michromaFontFamily,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = michromaFontFamily,
                    baselineShift = BaselineShift(.3f)
                )
            )
            SettingsMenuLink(
                title = {
                    Text(text = "Xmp Android (this fork)")
                },
                subtitle = {
                    Text(text = "https://github.com/LossyDragon/xmp-android")
                },
                onClick = { uriHandler.openUri("https://github.com/LossyDragon/xmp-android") }
            )
            SettingsMenuLink(
                title = { Text(text = "Xmp Android (Original)") },
                subtitle = { Text(text = "https://github.com/cmatsuoka/xmp-android") },
                onClick = { uriHandler.openUri("https://github.com/cmatsuoka/xmp-android") }
            )
            SettingsMenuLink(
                title = { Text(text = "Libxmp") },
                subtitle = { Text(text = "https://github.com/libxmp/libxmp/") },
                onClick = { uriHandler.openUri("https://github.com/libxmp/libxmp/") }
            )
        }
    }
}

@Composable
private fun AboutText(
    string: String,
    textAlign: TextAlign = TextAlign.Center,
    topPadding: Dp = 4.dp,
    bottomPadding: Dp = 4.dp,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified
) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = bottomPadding),
        text = string,
        textAlign = textAlign,
        style = style,
        color = color
    )
}

@Preview
@Composable
private fun Preview_AboutScreen() {
    XmpTheme(useDarkTheme = true) {
        AboutScreen(
            buildVersionName = "1.2.3",
            buildVersionCode = 669,
            libVersion = "4.5.6",
            onBack = { }
        )
    }
}
