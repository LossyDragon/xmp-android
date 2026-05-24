package com.lossydragon.media3.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import com.lossydragon.media3.ui.theme.XmpTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProgressbarIndicator(isLoading: Boolean = true) {
    if (!isLoading) return

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 3.dp,
        content = {
            Box(
                modifier = Modifier.padding(48.dp),
                contentAlignment = Alignment.Center,
                content = { LoadingIndicator(modifier = Modifier.size(56.dp)) }
            )
        }
    )
}

@Preview
@Composable
private fun ProgressbarIndicatorPreview() {
    XmpTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = { ProgressbarIndicator(isLoading = true) }
        )
    }
}
