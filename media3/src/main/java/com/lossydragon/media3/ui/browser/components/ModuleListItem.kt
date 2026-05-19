package com.lossydragon.media3.ui.browser.components

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.ui.theme.XmpTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.helllabs.libxmp.Xmp
import org.helllabs.libxmp.model.ModInfo

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ModuleListItem(
    file: ModuleFile,
    onClick: () -> Unit,
    getCached: (String) -> ModInfo,
    onCache: (String, ModInfo) -> Unit
) {
    val context = LocalContext.current
    val uriStr = file.uri.toString()

    val cached = remember(file) { getCached(uriStr) }
    var itemName by remember(file) { mutableStateOf(cached.name.ifEmpty { file.name }) }
    var itemType by remember(file) { mutableStateOf(cached.type.ifEmpty { file.extension }) }

    LaunchedEffect(file) {
        if (cached.name.isNotBlank()) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            // Prevent when fast scrolling, will auto cancel
            delay(100)

            Xmp.testFromFd(context, file.uri, cached)

            onCache(uriStr, cached)

            withContext(Dispatchers.Main) {
                // First time it won't trim, do it again.
                itemName = cached.name.trim().ifBlank { file.name }
                itemType = cached.type.ifBlank { file.extension }
            }
        }
    }

    ListItem(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        content = {
            AnimatedContent(
                targetState = itemName,
                label = "module_name",
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(150))
                },
            ) { name ->
                Text(
                    text = name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        supportingContent = {
            AnimatedContent(
                targetState = itemType,
                label = "module_type",
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(150))
                },
            ) { type ->
                Text(text = type)
            }
        },
        leadingContent = {
            Icon(Icons.Default.AudioFile, contentDescription = null)
        },
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
                getCached = { ModInfo() },
                onCache = { _, _ -> },
            )
        }
    }
}
