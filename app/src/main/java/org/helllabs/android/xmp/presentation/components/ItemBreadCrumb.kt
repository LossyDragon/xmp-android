package org.helllabs.android.xmp.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.presentation.theme.AppTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemBreadCrumb(
    crumb: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .padding(4.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(12.dp),
                imageVector = Icons.Default.FolderOpen,
                contentDescription = "Playlist Icon"
            )
            Text(
                modifier = Modifier.padding(start = 2.dp),
                text = crumb,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
fun ItemBreadCrumbPreview() {
    AppTheme(false) {
        ItemBreadCrumb(
            crumb = "Some Bread Crumb",
            onClick = {},
            onLongClick = {}
        )
    }
}

@Preview
@Composable
fun ItemBreadCrumbPreviewDark() {
    AppTheme(true) {
        ItemBreadCrumb(
            crumb = "Some Bread Crumb",
            onClick = {},
            onLongClick = {}
        )
    }
}
