package org.helllabs.android.xmp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.google.accompanist.insets.statusBarsPadding
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.theme.XmpTheme
import org.helllabs.android.xmp.ui.theme.michromaFontFamily
import org.helllabs.android.xmp.ui.theme.themedText

@Composable
fun AppBar(
    title: String,
    navIconClick: (() -> Unit)? = null,
    menuActions: @Composable RowScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxWidth()) {
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
    Column(Modifier.fillMaxWidth()) {
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
private fun AppBarText(
    appbarTitle: AnnotatedString,
    titleClick: (() -> Unit)?
) {
    Row {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = titleClick != null,
                    onClick = { titleClick?.invoke() }
                ),
            text = appbarTitle,
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
