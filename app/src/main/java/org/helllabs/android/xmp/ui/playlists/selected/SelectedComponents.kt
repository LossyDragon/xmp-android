package org.helllabs.android.xmp.ui.playlists.selected

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.listItems
import com.vanpra.composematerialdialogs.title
import org.helllabs.android.xmp.ui.components.TwoLineItem

@Composable
fun SelectedItemCard(
    modifier: Modifier = Modifier,
    elevation: Dp,
    primaryText: String,
    secondaryText: String,
    onClick: () -> Unit,
    onOverFlow: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    TwoLineItem(
        modifier = modifier,
        elevation = elevation,
        primaryText = primaryText,
        secondaryText = secondaryText,
        onItemClick = onClick,
        onOverflow = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onOverFlow()
        },
        cardIcon = Icons.Default.InsertDriveFile
    )
}

@Composable
fun ItemEditDialog(
    dialogState: MaterialDialogState,
    onClick: (index: Int) -> Unit
) {
    MaterialDialog(dialogState = dialogState) {
        title("Edit Playlist")
        listItems(
            list = listOf("Play starting here", "Add to queue", "Remove"),
            onClick = { index, _ ->
                onClick(index)
            }
        )
    }
}
