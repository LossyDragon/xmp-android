package org.helllabs.android.xmp.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import org.helllabs.android.xmp.ui.theme.darkAccent

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
