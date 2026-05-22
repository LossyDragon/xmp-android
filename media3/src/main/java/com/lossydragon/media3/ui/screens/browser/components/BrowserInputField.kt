package com.lossydragon.media3.ui.screens.browser.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.tooling.preview.*
import com.lossydragon.media3.model.BrowserSortOrder
import com.lossydragon.media3.ui.theme.XmpTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BrowserInputField(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    colors: TextFieldColors,
    sortOrder: BrowserSortOrder,
    onSortOrder: (BrowserSortOrder) -> Unit,
    onFolderPick: () -> Unit,
    onFilter: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    SearchBarDefaults.InputField(
        modifier = Modifier.fillMaxWidth(),
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        colors = colors,
        onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
        placeholder = { Text("Search modules…") },
        leadingIcon = {
            if (searchBarState.currentValue == SearchBarValue.Expanded) {
                IconButton(
                    onClick = {
                        scope.launch { searchBarState.animateToCollapsed() }
                        textFieldState.clearText()
                        onFilter("")
                    },
                    content = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                )
            } else {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            }
        },
        trailingIcon = {
            if (textFieldState.text.isNotEmpty()) {
                IconButton(
                    onClick = {
                        textFieldState.clearText()
                        onFilter("")
                    },
                    content = {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                    }
                )
            } else {
                Row {
                    IconButton(
                        onClick = onFolderPick,
                        content = {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                        }
                    )
                    SortMenu(sortOrder = sortOrder, onSortOrder = onSortOrder)
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview() {
    val searchBarState = rememberSearchBarWithGapState()
    val textFieldState = rememberTextFieldState()
    val appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(
        scrolledSearchBarContainerColor = Color.Unspecified,
        scrolledAppBarContainerColor = Color.Unspecified,
    )
    XmpTheme {
        Surface {
            BrowserInputField(
                textFieldState = textFieldState,
                searchBarState = searchBarState,
                colors = appBarWithSearchColors.searchBarColors.inputFieldColors,
                sortOrder = BrowserSortOrder.SIZE,
                onSortOrder = {},
                onFolderPick = {},
                onFilter = {},
            )
        }
    }
}
