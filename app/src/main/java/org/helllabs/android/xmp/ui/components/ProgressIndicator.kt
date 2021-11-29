package org.helllabs.android.xmp.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.ui.theme.XmpTheme3

@Composable
fun ProgressbarIndicator(isLoading: Boolean = true) {
    if (isLoading) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 1f),
            shadowElevation = 8.dp
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(64.dp)
                    .scale(2f),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Light Theme", showBackground = true)
@Composable
private fun ProgressbarIndicatorPreview() {
    XmpTheme3 {
        Surface {
            Column(
                Modifier.fillMaxSize(),
                Arrangement.Center,
                Alignment.CenterHorizontally
            ) {
                ProgressbarIndicator(isLoading = true)
            }
        }
    }
}
