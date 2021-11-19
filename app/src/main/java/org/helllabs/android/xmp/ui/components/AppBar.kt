package org.helllabs.android.xmp.ui.components

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.insets.systemBarsPadding
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.ui.theme.michromaFontFamily
import org.helllabs.android.xmp.ui.theme.themedText

@Composable
fun XmpAppBar3(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onNavIconPressed: (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    titleText: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val backgroundColors = TopAppBarDefaults.centerAlignedTopAppBarColors()
    val backgroundColor = backgroundColors.containerColor(
        scrollFraction = scrollBehavior?.scrollFraction ?: 0f
    ).value
    val foregroundColors = TopAppBarDefaults.smallTopAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent
    )

    // Top App Bar
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val appBarModifier =
        if (isPortrait) modifier.statusBarsPadding() else modifier.systemBarsPadding()

    Box(modifier = Modifier.background(backgroundColor)) {
        SmallTopAppBar(
            modifier = appBarModifier,
            actions = actions,
            title = {
                titleText?.let {
                    AppBarText(buildAnnotatedString { append(titleText) })
                }
                title?.let {
                    title()
                }
            },
            scrollBehavior = scrollBehavior,
            colors = foregroundColors,
            navigationIcon = {
                onNavIconPressed?.let {
                    IconButton(
                        modifier = Modifier
                            .size(64.dp)
                            .clickable(onClick = onNavIconPressed)
                            .padding(16.dp),
                        onClick = { onNavIconPressed() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun AppBarText(
    title: AnnotatedString,
    titleClick: (() -> Unit)? = {}
) {
    Row {
        Text(
            modifier = Modifier
                .clickable(
                    enabled = titleClick != null,
                    onClick = { titleClick?.invoke() }
                ),
            text = title,
            textAlign = TextAlign.Start,
            fontFamily = michromaFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            // IDK how I found this "Clip to Padding" hack...
            style = TextStyle(baselineShift = BaselineShift(.3f)),
        )
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Light Theme", uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun XmpAppBar3Preview() {
    XmpTheme3 {
        XmpAppBar3(
            title = { AppBarText(themedText(R.string.app_name)) },
            onNavIconPressed = {},
            actions = { PlaylistMenuItems({}, {}) }
        )
    }
}
