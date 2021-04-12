package org.helllabs.android.xmp.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import org.helllabs.android.xmp.presentation.theme.AppTheme

@Composable
fun ProgressbarIndicator(
    isLoading: Boolean
) {
    if (isLoading) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.scale(2f)
            )
        }
    }
}

/************
 * Previews *
 ************/

@Preview
@Composable
fun ProgressbarIndicatorPreview() {
    AppTheme(false) {
        ProgressbarIndicator(isLoading = true)
    }
}
