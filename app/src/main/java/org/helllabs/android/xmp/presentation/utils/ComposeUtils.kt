package org.helllabs.android.xmp.presentation.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import org.helllabs.android.xmp.presentation.theme.darkAccent

/* Internal for Compose Previewing */
fun internalTextGenerator(): String {
    return "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
        "sed do eiusmod tempor incididunt ut labore et dolore " +
        "magna aliqua. Ut enim ad minim veniam, quis nostrud " +
        "exercitation ullamco laboris nisi ut aliquip ex ea " +
        "commodo consequat. Duis aute irure dolor in " +
        "reprehenderit in voluptate velit esse cillum " +
        "dolore eu fugiat nulla pariatur. Excepteur sint " +
        "occaecat cupidatat non proident, sunt in culpa " +
        "qui officia deserunt mollit anim id est laborum."
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
