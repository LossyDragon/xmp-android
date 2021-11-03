package org.helllabs.android.xmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.theme.XmpTheme
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

    Box(modifier = Modifier.background(backgroundColor)) {
        SmallTopAppBar(
            modifier = modifier,
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
                    androidx.compose.material3.IconButton(
                        modifier = Modifier
                            .size(64.dp)
                            .clickable(onClick = onNavIconPressed)
                            .padding(16.dp),
                        onClick = { onNavIconPressed() }
                    ) {
                        androidx.compose.material3.Icon(
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
fun AppBar(
    title: String,
    navIconClick: (() -> Unit)? = null,
    menuActions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding(bottom = false)
    ) {
        TopAppBar(
            modifier = Modifier.statusBarsPadding(),
            backgroundColor = Color.Transparent,
            contentColor = MaterialTheme.colors.onSurface,
            actions = menuActions,
            elevation = 0.dp,
            navigationIcon = {
                navIconClick?.let {
                    IconButton(onClick = { navIconClick() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                }
            },
            title = { AppBarText(buildAnnotatedString { append(title) }, null) },
        )
        Divider()
    }
}

@Composable
fun AppBar(
    annotatedTitle: AnnotatedString,
    menuActions: @Composable RowScope.() -> Unit = {},
    titleClick: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding(bottom = false)
    ) {
        TopAppBar(
            modifier = Modifier.statusBarsPadding(),
            backgroundColor = Color.Transparent,
            contentColor = MaterialTheme.colors.onSurface,
            actions = menuActions,
            elevation = 0.dp,
            title = { AppBarText(annotatedTitle, titleClick) },
        )
        Divider()
    }
}

@Composable
fun AppBarText(
    title: AnnotatedString,
    titleClick: (() -> Unit)? = {}
) {
    Row {
        androidx.compose.material3.Text(
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

@Preview(name = "Material 3 Light/Dark Theme")
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

@Preview(name = "Light/Dark Theme")
@Composable
private fun AppBarPreview() {
    XmpTheme(false) {
        AppBar(
            title = stringResource(id = R.string.app_name),
            navIconClick = {},
        )
    }
}

@Preview(name = "Light/Dark Theme")
@Composable
private fun AppBarPreviewDark() {
    XmpTheme(true) {
        AppBar(
            annotatedTitle = themedText(R.string.app_name),
        )
    }
}
