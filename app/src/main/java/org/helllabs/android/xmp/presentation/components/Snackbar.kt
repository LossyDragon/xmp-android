package org.helllabs.android.xmp.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.util.upperCase

@Composable
fun DialogSnackbar(
    snackBarState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    SnackbarHost(
        modifier = modifier,
        hostState = snackBarState,
        snackbar = { snack ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                content = { Text(text = snack.message) },
                action = {
                    snack.actionLabel?.let {
                        TextButton(
                            onClick = { onDismiss() }
                        ) {
                            Text(
                                text = it.upperCase(),
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.secondary
                                )
                            )
                        }
                    }
                }
            )
        }
    )
}
