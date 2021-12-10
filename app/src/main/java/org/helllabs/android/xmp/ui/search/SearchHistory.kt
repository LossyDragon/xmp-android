package org.helllabs.android.xmp.ui.search

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.ui.NavScreens
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.util.DialogMessage
import org.helllabs.android.xmp.util.PrefManager

@Composable
fun SearchHistoryScreen(
    navController: NavController,
    viewModel: SearchHistoryViewModel = hiltViewModel()
) {
    // Only here to force recompositions
    val list = remember { mutableStateOf(viewModel.historyList) }

    SearchHistoryLayout(
        onBack = { navController.popBackStack() },
        onClick = { id ->
            navController.navigate(
                NavScreens.SearchModuleResult.route + "?moduleId=$id"
            )
        },
        historyList = list.value,
        onCleared = {
            PrefManager.clearSearchHistory()
            list.value = listOf()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchHistoryLayout(
    onBack: () -> Unit,
    onClick: (id: Int) -> Unit,
    historyList: List<Module>,
    onCleared: () -> Unit,
) {
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    val onClearState = rememberMaterialDialogState()
    DialogMessage(
        dialogState = onClearState,
        title = R.string.dialog_clear_history_title,
        message = R.string.dialog_clear_history_msg,
        positiveButtonText = R.string.delete,
        negativeButtonText = R.string.cancel,
        onPositiveButton = onCleared,
        onDismiss = { onClearState.hide() }
    )

    Scaffold(
        topBar = {
            XmpAppBar3(
                scrollBehavior = scrollBehavior,
                onNavIconPressed = onBack,
                titleText = stringResource(id = R.string.search_history),
                actions = {
                    if (historyList.isNotEmpty()) {
                        DeleteMenu(
                            deleteClick = { onClearState.show() },
                            image = Icons.Default.ClearAll
                        )
                    }
                }
            )
        }
    ) {
        LazyList(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            boxContent = {
                if (historyList.isEmpty()) {
                    ErrorLayout(message = stringResource(id = R.string.history_no_items))
                }
            },
            lazyContent = {
                itemsIndexed(items = historyList.reversed()) { _, item ->
                    ItemModule(
                        item = item,
                        onClick = { onClick(item.id) }
                    )
                }
            }
        )
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Light Theme", uiMode = UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun SearchHistoryPreviewDark() {
    val list = fakeDataSearchListResult().module
    XmpTheme3 {
        SearchHistoryLayout(
            onBack = {},
            onClick = {},
            historyList = list,
            onCleared = {},
        )
    }
}
