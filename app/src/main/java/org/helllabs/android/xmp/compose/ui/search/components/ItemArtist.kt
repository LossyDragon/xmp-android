package org.helllabs.android.xmp.compose.ui.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.helllabs.android.xmp.compose.theme.XmpTheme

@Composable
fun ItemArtist(
    alias: String,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            modifier = Modifier.clickable { onClick() },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ),
            headlineContent = {
                Text(
                    text = alias,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        )
    }
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        ItemArtist(
            alias = "Artist Alias",
            onClick = { },
        )
    }
}
