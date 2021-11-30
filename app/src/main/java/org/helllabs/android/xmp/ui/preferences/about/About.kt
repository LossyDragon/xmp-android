package org.helllabs.android.xmp.ui.preferences.about

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsPadding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.ui.components.XmpAppBar3
import org.helllabs.android.xmp.ui.components.waterfallPadding
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.ui.theme.michromaFontFamily
import org.helllabs.android.xmp.ui.theme.themedText
import org.helllabs.android.xmp.util.AppTheme
import org.helllabs.android.xmp.util.PrefTheme
import org.helllabs.android.xmp.util.logD

@AndroidEntryPoint
class About : ComponentActivity() {

    @Inject
    lateinit var prefTheme: PrefTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        logD("onCreate")
        setContent {
            val theme = prefTheme.themeStream.collectAsState()
            val themeMode = when (theme.value) {
                AppTheme.MODE_AUTO -> isSystemInDarkTheme()
                AppTheme.MODE_DAY -> false
                AppTheme.MODE_NIGHT -> true
            }

            ProvideWindowInsets {
                AboutLayout(
                    onBack = { onBackPressed() },
                    isDarkTheme = themeMode,
                    appVersion = BuildConfig.VERSION_NAME,
                    xmpVersion = Xmp.getVersion()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutLayout(
    onBack: () -> Unit,
    isDarkTheme: Boolean,
    appVersion: String,
    xmpVersion: String,
) {
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    XmpTheme3(isDarkTheme = isDarkTheme) {
        Scaffold(
            topBar = {
                XmpAppBar3(
                    scrollBehavior = scrollBehavior,
                    titleText = stringResource(id = R.string.pref_about_title),
                    onNavIconPressed = onBack
                )
            }
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .waterfallPadding()
                    .navigationBarsPadding()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = themedText(R.string.app_name),
                    textAlign = TextAlign.Center,
                    fontFamily = michromaFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(baselineShift = BaselineShift(.3f)),
                )
                AboutText(stringResource(id = R.string.about_version, appVersion))
                AboutText(stringResource(id = R.string.about_author))
                AboutText(stringResource(id = R.string.about_xmp, xmpVersion))
                Spacer(modifier = Modifier.height(8.dp))
                Divider(
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
                    style = TextStyle(baselineShift = BaselineShift(.3f)),
                )
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

@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AboutLayoutPreview() {
    AboutLayout(
        onBack = { },
        isDarkTheme = true,
        appVersion = "00.00.00",
        xmpVersion = "6.6.9",
    )
}
