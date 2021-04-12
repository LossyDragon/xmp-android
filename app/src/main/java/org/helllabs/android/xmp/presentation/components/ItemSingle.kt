package org.helllabs.android.xmp.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.presentation.theme.AppTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemSingle(
    text: String,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = { onLongClick?.invoke() }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            text = text
        )
    }
}

/************
 * Previews *
 ************/

@Preview
@Composable
fun ItemSinglePreview() {
    AppTheme(false) {
        ItemSingle("Single Item", {}, {})
    }
}

@Preview
@Composable
fun ItemSinglePreviewDark() {
    AppTheme(true) {
        ItemSingle("Single Item", {}, {})
    }
}
