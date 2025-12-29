package org.helllabs.android.xmp.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.theme.XmpTheme

@Composable
fun ErrorScreen(
    modifier: Modifier = Modifier,
    text: String?,
    action: (@Composable () -> Unit)? = null
) {
    if (text.isNullOrEmpty()) {
        return
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 3.dp, // Use tonal elevation
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.frowny_face),
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.fillMaxWidth(.75f),
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            action?.let {
                it()
            }
        }
    }
}

@Preview
@Composable
private fun Preview_ErrorScreen() {
    XmpTheme(useDarkTheme = true) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = {
                ErrorScreen(text = "Some very long error message that should wrap around")
            }
        )
    }
}
