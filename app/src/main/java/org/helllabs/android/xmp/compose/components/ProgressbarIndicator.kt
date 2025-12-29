package org.helllabs.android.xmp.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.compose.theme.XmpTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProgressbarIndicator(isLoading: Boolean = true) {
    if (isLoading) {
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
}

@Preview
@Composable
private fun ProgressbarIndicatorPreview() {
    XmpTheme(useDarkTheme = true) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = {
                ProgressbarIndicator(isLoading = true)
            }
        )
    }
}
