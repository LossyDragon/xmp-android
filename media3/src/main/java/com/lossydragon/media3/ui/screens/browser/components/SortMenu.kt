package com.lossydragon.media3.ui.screens.browser.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.lossydragon.media3.model.BrowserSortOrder
import com.lossydragon.media3.ui.theme.XmpTheme

@Composable
internal fun SortMenu(
    sortOrder: BrowserSortOrder,
    onSortOrder: (BrowserSortOrder) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { showSortMenu = true },
            content = {
                Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
            }
        )
        DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false },
            shape = MaterialTheme.shapes.small,
            content = {
                DropdownMenuItem(
                    text = { Text(text = "Name") },
                    onClick = {
                        onSortOrder(BrowserSortOrder.NAME)
                        showSortMenu = false
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.SortByAlpha, contentDescription = null)
                    },
                    trailingIcon = {
                        if (sortOrder == BrowserSortOrder.NAME) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = "Type") },
                    onClick = {
                        onSortOrder(BrowserSortOrder.TYPE)
                        showSortMenu = false
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Extension, contentDescription = null)
                    },
                    trailingIcon = {
                        if (sortOrder == BrowserSortOrder.TYPE) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = "Size") },
                    onClick = {
                        onSortOrder(BrowserSortOrder.SIZE)
                        showSortMenu = false
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.DataArray, contentDescription = null)
                    },
                    trailingIcon = {
                        if (sortOrder == BrowserSortOrder.SIZE) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        }
                    }
                )
            }
        )
    }
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        Surface {
            SortMenu(
                sortOrder = BrowserSortOrder.SIZE,
                onSortOrder = {}
            )
        }
    }
}
