package org.helllabs.android.xmp.presentation.ui.preferences

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.navigationBarsPadding
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.presentation.components.AppBar
import org.helllabs.android.xmp.presentation.theme.*
import org.helllabs.android.xmp.util.logD

class About : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        logD("onCreate")
        setContent {
            AboutLayout(
                onBack = { onBackPressed() },
                appVersion = BuildConfig.VERSION_NAME,
                xmpVersion = Xmp.getVersion()
            )
        }
    }
}

@Composable
private fun AboutLayout(
    isDarkTheme: Boolean = systemDarkTheme(),
    onBack: () -> Unit,
    appVersion: String,
    xmpVersion: String,
) {
    AppTheme(
        isDarkTheme = isDarkTheme
    ) {
        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(id = R.string.pref_about_title),
                    navIconClick = { onBack() },
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = themedText(R.string.app_name),
                    textAlign = TextAlign.Center,
                    fontFamily = michromaFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    // IDK how I found this "Clip to Padding" hack...
                    style = TextStyle(baselineShift = BaselineShift(.3f)),
                )
                AboutText(stringResource(id = R.string.about_version, appVersion))
                AboutText(stringResource(id = R.string.about_author))
                AboutText(stringResource(id = R.string.about_xmp, xmpVersion))
                Divider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                AboutText(stringResource(id = R.string.changelog))
                AboutText(stringResource(id = R.string.changelog_text), TextAlign.Start)
            }
        }
    }
}

@Composable
private fun AboutText(string: String, textAlign: TextAlign = TextAlign.Center) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        text = string,
        textAlign = textAlign,
    )
}

/************
 * Previews *
 ************/

@Preview
@Composable
private fun AboutLayoutPreview() {
    AboutLayout(
        isDarkTheme = false,
        onBack = { },
        appVersion = "00.00.00",
        xmpVersion = "6.6.9",
    )
}

@Preview
@Composable
private fun AboutLayoutPreviewDark() {
    AboutLayout(
        isDarkTheme = true,
        onBack = { },
        appVersion = "00.00.00",
        xmpVersion = "6.6.9",
    )
}
