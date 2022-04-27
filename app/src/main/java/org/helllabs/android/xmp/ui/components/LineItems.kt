package org.helllabs.android.xmp.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.ui.theme.XmpAndroidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoLineItem(
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp,
    primaryText: String,
    secondaryText: String,
    onItemClick: () -> Unit,
    onOverflow: () -> Unit,
    cardIcon: ImageVector,
    cardOverFlow: ImageVector = Icons.Default.MoreVert,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp)
            .heightIn(min = 72.dp),
        tonalElevation = elevation,
        color = MaterialTheme.colorScheme.inverseOnSurface,
        onClick = onItemClick,
        shape = RoundedCornerShape(6.dp),
    ) {
        TwoLineContent(
            primaryText = primaryText,
            secondaryText = secondaryText,
            onOverflow = onOverflow,
            cardIcon = cardIcon,
            cardOverFlow = cardOverFlow
        )
    }
}

@Composable
private fun TwoLineContent(
    primaryText: String,
    secondaryText: String,
    onOverflow: () -> Unit,
    cardIcon: ImageVector,
    cardOverFlow: ImageVector,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(40.dp),
            imageVector = cardIcon,
            contentDescription = "Line Item Icon"
        )
        Column(
            modifier = Modifier
                .fillMaxWidth(.85f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = primaryText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = secondaryText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            modifier = Modifier.padding(end = 16.dp),
            onClick = onOverflow
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = cardOverFlow,
                contentDescription = "Line Item Overflow Icon"
            )
        }
    }
}

@Preview(name = "Two Line Item Dark", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Two Line Item Light", uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun TwoLineItem_Preview() {
    XmpAndroidTheme {
        TwoLineItem(
            primaryText = "Some Title - Some Title - Some Title",
            secondaryText = "Come Comment - Come Comment - Come Comment - Come Comment - ",
            cardIcon = Icons.Default.Warning,
            onItemClick = {},
            onOverflow = {}
        )
    }
}
