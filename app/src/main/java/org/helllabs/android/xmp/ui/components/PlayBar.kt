package org.helllabs.android.xmp.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.theme.XmpAndroidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerBottomBar(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    name: String,
    type: String,
    onBarClick: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onForward: () -> Unit,
) {
    Surface(
        modifier = modifier.height(64.dp).padding(bottom = 6.dp),
        onClick = onBarClick,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomBarContent(Modifier.weight(1f), name, type)
            BottomBarButtons(
                isPlaying = isPlaying,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onForward = onForward
            )
        }
    }
}

@Composable
private fun BottomBarContent(
    modifier: Modifier,
    name: String,
    type: String
) {
    Image(
        modifier = Modifier
            .size(48.dp)
            .fillMaxHeight(),
        painter = painterResource(id = R.drawable.ic_launcher_foreground),
        contentDescription = null,
    )
    Column(
        modifier = modifier,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(0.dp))
        Text(
            text = type,
            style = MaterialTheme.typography.labelSmall,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun BottomBarButtons(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onForward: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Skip Previous"
            )
        }
        IconButton(onClick = onPlayPause) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play or Pause"
            )
        }
        IconButton(onClick = onForward) {
            Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Skip Forward")
        }
    }
}

@Preview(name = "Player Bottom Bar Dark", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Player Bottom Bar Light", uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun PlayerBottomBar_Preview() {
    XmpAndroidTheme {
        PlayerBottomBar(
            onBarClick = {},
            name = "Some Title",
            type = "Scream Tracker 3",
            isPlaying = true,
            onPrevious = {},
            onPlayPause = {},
            onForward = {},
        )
    }
}
