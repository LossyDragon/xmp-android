package com.lossydragon.media3.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import com.lossydragon.media3.ui.screens.player.PlayerScreen

@Composable
fun XmpNavHost(onBack: () -> Unit) {
    val rootBackStack = rememberNavBackStack(NavKeyRoot.Main)

    NavDisplay(
        backStack = rootBackStack,
        onBack = { rootBackStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<NavKeyRoot.Main> {
                MainNavigation(
                    onNavigateToPlayer = {
                        if (rootBackStack.lastOrNull() !is NavKeyRoot.Player) {
                            rootBackStack.add(NavKeyRoot.Player)
                        }
                    },
                    onBack = onBack,
                )
            }
            entry<NavKeyRoot.Player> {
                PlayerScreen(
                    onBack = rootBackStack::removeLastOrNull
                )
            }
        }
    )
}
