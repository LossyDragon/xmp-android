package com.lossydragon.media3.ui.browser.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lossydragon.media3.ui.theme.XmpTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun BreadCrumbs(
    modifier: Modifier = Modifier,
    breadcrumbs: ImmutableList<String>,
    onCrumbClick: (Int) -> Unit
) {
    val scrollState = rememberLazyListState()

    // Auto-scroll to end
    LaunchedEffect(breadcrumbs.size) {
        if (breadcrumbs.isNotEmpty()) {
            scrollState.animateScrollToItem(breadcrumbs.lastIndex)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            state = scrollState,
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = 8.dp),
            content = {
                itemsIndexed(breadcrumbs) { index, crumb ->
                    val isLast = index == breadcrumbs.lastIndex
                    AssistChip(
                        modifier = Modifier.padding(horizontal = 2.dp),
                        enabled = !isLast,
                        onClick = { onCrumbClick(index) },
                        label = {
                            Text(
                                text = crumb,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        trailingIcon = if (!isLast) {
                            {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        )
    }
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        BreadCrumbs(
            breadcrumbs = listOf("AAA", "BBB", "CCC", "DDD", "EEE", "Mod Player").toImmutableList(),
            onCrumbClick = {},
        )
    }
}
