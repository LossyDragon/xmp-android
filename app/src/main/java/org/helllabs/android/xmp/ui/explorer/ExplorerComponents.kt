package org.helllabs.android.xmp.ui.explorer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import org.helllabs.android.xmp.ui.components.TwoLineItem

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
