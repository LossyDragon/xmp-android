package com.lossydragon.media3.util

import android.text.Html

/** Formats milliseconds as `m:ss`. */
fun Long.formatMs(): String {
    val s = this / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/** Formats bytes as a human-readable size string (B, KB, MB). */
fun Long.formatSize(): String = when {
    this < 1_024L -> "$this B"
    this < 1_048_576L -> "${"%.1f".format(this / 1_024.0)} KB"
    else -> "${"%.1f".format(this / 1_048_576.0)} MB"
}

/** Strips HTML tags and decodes HTML entities. */
fun String.fromHtml(): String =
    Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString().trim()
