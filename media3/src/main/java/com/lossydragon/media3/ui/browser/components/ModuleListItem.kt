package com.lossydragon.media3.ui.browser.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.ui.theme.XmpTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ModuleListItem(
    file: ModuleFile,
    onClick: () -> Unit
) {
    ListItem(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        content = {
            Text(
                text = file.resolvedName.ifBlank { file.name },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        supportingContent = { Text(text = file.resolvedType.ifBlank { file.extension }) },
        leadingContent = { Icon(imageVector = Icons.Default.AudioFile, contentDescription = null) },
    )
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        Surface {
            ModuleListItem(
                file = ModuleFile(
                    uri = Uri.EMPTY,
                    name = "Preview",
                    sizeBytes = 123456L,
                    extension = ".669",
                ),
                onClick = {},
            )
        }
    }
}
