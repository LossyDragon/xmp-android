package org.helllabs.android.xmp.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.toPaddingValues
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LazyColumnWithTopScroll(
    modifier: Modifier,
    fabModifier: Modifier = Modifier,
    showScrollAt: Int,
    shouldPadBottom: Boolean = true,
    additionalBottomPad: Dp = 80.dp,
    boxContent: @Composable BoxScope.() -> Unit,
    lazyContent: LazyListScope.() -> Unit
) {
    Box(modifier = modifier) {
        val listState = rememberLazyListState()
        val showScrollButton = listState.firstVisibleItemIndex > showScrollAt
        val scope = rememberCoroutineScope()

        LazyColumn(
            state = listState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentPadding = LocalWindowInsets.current.systemBars.toPaddingValues(
                top = false,
                bottom = shouldPadBottom,
                additionalTop = 10.dp,
                additionalBottom = additionalBottomPad,
            ),
            content = lazyContent
        )

        boxContent()

        // Show smooth scroll up button
        AnimatedVisibility(
            visible = showScrollButton,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            ScrollFab(
                modifier = fabModifier,
                onClick = {
                    scope.launch { listState.animateScrollToItem(0) }
                },
                shouldPadBottom = shouldPadBottom
            )
        }
    }
}
