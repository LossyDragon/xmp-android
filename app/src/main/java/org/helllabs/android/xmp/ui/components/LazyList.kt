package org.helllabs.android.xmp.ui.components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import kotlinx.coroutines.launch

private val ScrollThreshold = 56.dp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LazyList(
    modifier: Modifier,
    scrollModifier: Modifier = Modifier,
    shouldPadBottom: Boolean = true,
    additionalBottomPad: Dp = 80.dp,
    boxContent: @Composable BoxScope.() -> Unit,
    lazyContent: LazyListScope.() -> Unit
) {
    Box(modifier = modifier) {
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        LazyColumn(
            state = listState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentPadding = rememberInsetsPaddingValues(
                insets = LocalWindowInsets.current.systemBars,
                applyTop = false,
                applyBottom = shouldPadBottom,
                additionalTop = 10.dp,
                additionalBottom = additionalBottomPad,
            ),
            content = lazyContent
        )

        boxContent()

        val scrollThreshold = with(LocalDensity.current) {
            ScrollThreshold.toPx()
        }

        val scrollButtonEnabled by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex != 0 ||
                    listState.firstVisibleItemScrollOffset > scrollThreshold
            }
        }

        ScrollBackUp(
            modifier = scrollModifier.align(Alignment.BottomCenter),
            enabled = scrollButtonEnabled,
            onClicked = {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            },
        )
    }
}
