package org.helllabs.android.xmp.ui.explorer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import org.helllabs.android.xmp.ui.components.TwoLineItem
import org.helllabs.android.xmp.ui.theme.XmpAndroidTheme

@Composable
fun ExplorerItemCard(
    modifier: Modifier = Modifier,
    cardIcon: ImageVector,
    primaryText: String,
    secondaryText: String,
    onClick: () -> Unit,
    onOverFlow: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    TwoLineItem(
        modifier = modifier,
        primaryText = primaryText,
        secondaryText = secondaryText,
        onItemClick = onClick,
        onOverflow = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onOverFlow()
        },
        cardIcon = cardIcon
    )
}

@Preview
@Composable
private fun ExplorerItemCard_Preview() {
    XmpAndroidTheme {
        ExplorerItemCard(
            cardIcon = Icons.Default.Folder,
            primaryText = "Some Item",
            secondaryText = "Some Text",
            onClick = {},
            onOverFlow = {}
        )
    }
}
