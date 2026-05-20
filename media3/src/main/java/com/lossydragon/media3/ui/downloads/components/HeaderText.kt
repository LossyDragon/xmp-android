package com.lossydragon.media3.ui.downloads.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun HeaderText(text: String) {
    Text(
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
        text = text
    )
}
