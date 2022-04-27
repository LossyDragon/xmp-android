package org.helllabs.android.xmp.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import org.helllabs.android.xmp.ui.theme.darkAccent
import org.helllabs.android.xmp.ui.theme.michromaFontFamily

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

// Accent the "Xmp" part of the text, if we're on the main screen.
@Composable
fun themedText(@StringRes res: Int): AnnotatedString {
    val string = stringResource(id = res)
    return buildAnnotatedString {
        withStyle(style = SpanStyle(color = darkAccent)) {
            append(string.substring(0, 3))
        }

        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.onBackground
            )
        ) {
            append(string.substring(3, string.length))
        }
    }
}
