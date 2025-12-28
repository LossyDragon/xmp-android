package org.helllabs.android.xmp.compose.ui.preferences

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.Serializable
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(baselineShift = BaselineShift(.3f))
            )
            AboutText(
                string = stringResource(id = R.string.about_version, buildVersionName),
                bottomPadding = 0.dp,
                fontSize = 18.sp
            )
            AboutText(
                string = stringResource(id = R.string.about_version_code, buildVersionCode),
                topPadding = 0.dp,
                fontSize = 12.sp
            )
            AboutText(string = stringResource(id = R.string.about_author))
            AboutText(string = stringResource(id = R.string.about_xmp, libVersion))
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(.85f),
                color = MaterialTheme.colorScheme.inverseSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                text = stringResource(id = R.string.changelog),
                fontFamily = michromaFontFamily,
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(baselineShift = BaselineShift(.3f))
            )
            AboutText(string = stringResource(id = R.string.changelog_text))
        }
    }
}

@Composable
private fun AboutText(
    string: String,
    textAlign: TextAlign = TextAlign.Center,
    topPadding: Dp = 4.dp,
    bottomPadding: Dp = 4.dp,
    fontSize: TextUnit = TextUnit.Unspecified
) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = bottomPadding),
        text = string,
        textAlign = textAlign,
        fontSize = fontSize,
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
