package org.helllabs.android.xmp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import org.helllabs.android.xmp.ui.theme.XmpTheme

/**
 * RadioButton with Text and Sub-Text
 */
@Composable
fun RadioButtonItem(
    index: Int,
    selection: Int,
    text: String,
    subText: String? = null,
    radioButtonColors: RadioButtonColors = RadioButtonDefaults.colors(),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(
                selected = (index == selection),
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            colors = radioButtonColors,
            selected = (index == selection),
            onClick = null
        )
        Column(
            modifier = Modifier.padding(start = 16.dp),
            content = {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge
                )
                subText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )
    }
}

@Preview
@Composable
private fun Preview_RadioButtonItem() {
    XmpTheme(useDarkTheme = true) {
        Surface {
            Column {
                Array(3) {
                    RadioButtonItem(
                        index = 0,
                        selection = 1,
                        text = "Radio Button $it",
                        subText = "Radio Button Subtext $it",
                        onClick = { }
                    )
                }
            }
        }
    }
}
