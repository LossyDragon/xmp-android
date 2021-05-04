package org.helllabs.android.xmp.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.presentation.theme.AppTheme
import org.helllabs.android.xmp.presentation.theme.darkAccent
import org.helllabs.android.xmp.presentation.theme.lightGray

@Composable
fun RadioGroup(
    modifier: Modifier = Modifier,
    radioList: List<String>,
    selectedOption: Int,
    onSelected: (Int) -> Unit,
    overrideTheme: Boolean = false,
) {
    Column(
        modifier = modifier,
    ) {
        radioList.forEachIndexed { index, item ->
            RadioButtonItem(
                index = index,
                item = item,
                selectedOption = selectedOption,
                onSelected = { onSelected(it) },
                overrideTheme
            )
        }
    }
}

@Composable
fun RadioButtonItem(
    index: Int,
    item: String,
    selectedOption: Int,
    onSelected: (Int) -> Unit,
    overrideTheme: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected(index) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            modifier = Modifier.padding(6.dp),
            selected = index == selectedOption,
            onClick = { onSelected(index) },
            colors = RadioButtonDefaults.colors(
                selectedColor = darkAccent,
                unselectedColor = if (overrideTheme) lightGray
                else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        )
        Text(text = item, color = if (overrideTheme) lightGray else Color.Unspecified)
    }
}

/************
 * Previews *
 ************/

@Preview
@Composable
private fun RadioGroupPreview() {
    val list = listOf("Item 1", "Item 2", "Item 3")
    AppTheme(false) {
        RadioGroup(radioList = list, selectedOption = 0, onSelected = {})
    }
}

@Preview
@Composable
private fun RadioGroupPreviewDark() {
    AppTheme(true) {
        val list = listOf("Item 1", "Item 2", "Item 3")
        RadioGroup(radioList = list, selectedOption = 0, onSelected = {})
    }
}
