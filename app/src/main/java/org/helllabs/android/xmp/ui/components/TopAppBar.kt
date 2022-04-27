package org.helllabs.android.xmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues

@Composable
fun TopAppBar(
    actions: @Composable RowScope.() -> Unit = {},
    onNavPressed: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    title: @Composable () -> Unit,
) {
    val backgroundColors = TopAppBarDefaults.centerAlignedTopAppBarColors()
    val backgroundColor = backgroundColors.containerColor(
        scrollFraction = scrollBehavior?.scrollFraction ?: 0f
    ).value
    val foregroundColors = TopAppBarDefaults.smallTopAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent
    )
    val padding = rememberInsetsPaddingValues(
        LocalWindowInsets.current.statusBars,
        applyBottom = false,
    )
    Box(
        modifier = Modifier
            .background(backgroundColor)
            .padding(padding)
    ) {
        CenterAlignedTopAppBar(
            title = title,
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = foregroundColors,
            navigationIcon = {
                onNavPressed?.let {
                    IconButton(
                        modifier = Modifier
                            .size(64.dp)
                            .padding(16.dp),
                        onClick = { onNavPressed() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                }
            }
        )
    }
}

@Preview
@Composable
private fun TopAppBar_Preview() {
    TopAppBar(title = { Text("Xmp Mod Player") })
}
