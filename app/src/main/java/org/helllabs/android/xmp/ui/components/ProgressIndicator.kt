package org.helllabs.android.xmp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import org.helllabs.android.xmp.ui.theme.XmpTheme3

@Composable
fun ProgressbarIndicator(
    isLoading: Boolean = true
) {
    if (isLoading) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.scale(2f),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Light/Dark Theme")
@Composable
private fun ProgressbarIndicatorPreview() {
    XmpTheme3 {
        ProgressbarIndicator(isLoading = true)
    }
}
