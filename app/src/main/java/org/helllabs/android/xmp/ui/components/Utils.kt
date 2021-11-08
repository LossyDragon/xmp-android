package org.helllabs.android.xmp.ui.components

import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import org.helllabs.android.xmp.ui.theme.darkAccent

// https://github.com/egorikftp/compose-animated-bottomsheet
@OptIn(ExperimentalMaterialApi::class)
val BottomSheetScaffoldState.currentFraction: Float
    get() {
        val fraction = bottomSheetState.progress.fraction
        val targetValue = bottomSheetState.targetValue
        val currentValue = bottomSheetState.currentValue

        return when {
            currentValue == BottomSheetValue.Collapsed &&
                targetValue == BottomSheetValue.Collapsed -> 0f
            currentValue == BottomSheetValue.Expanded &&
                targetValue == BottomSheetValue.Expanded -> 1f
            currentValue == BottomSheetValue.Collapsed &&
                targetValue == BottomSheetValue.Expanded -> fraction
            else -> 1f - fraction
        }
    }

fun annotatedLinkString(
    linkString: String,
    toHyperLink: String,
): AnnotatedString = buildAnnotatedString {

    val startIndex = linkString.indexOf(toHyperLink)
    val endIndex = startIndex + toHyperLink.length

    val link = if (
        toHyperLink.contains("http://", true) ||
        toHyperLink.contains("https://", true)
    ) {
        toHyperLink
    } else {
        "https://$toHyperLink"
    }

    append(linkString)

    addStyle(
        style = SpanStyle(color = darkAccent, textDecoration = TextDecoration.Underline),
        start = startIndex,
        end = endIndex
    )

    addStringAnnotation(
        tag = "URL",
        annotation = link,
        start = startIndex,
        end = endIndex
    )
}

fun annotatedLink(
    string: String,
    url: String
): AnnotatedString = buildAnnotatedString {

    val startIndex = string.indexOf(string)
    val endIndex = startIndex + string.length

    append(string)

    addStyle(
        style = SpanStyle(color = darkAccent, textDecoration = TextDecoration.Underline),
        start = startIndex,
        end = endIndex
    )

    addStringAnnotation(
        tag = "URL",
        annotation = url,
        start = startIndex,
        end = endIndex
    )
}
