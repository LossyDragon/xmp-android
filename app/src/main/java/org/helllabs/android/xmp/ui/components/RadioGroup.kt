package org.helllabs.android.xmp.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.ui.theme.XmpTheme3

@Composable
fun RadioGroup(
    modifier: Modifier = Modifier,
    radioList: List<String>,
    selectedOption: Int,
    onSelected: (Int) -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        radioList.forEachIndexed { index, item ->
            RadioButtonItem(
                index = index,
                item = item,
                selectedOption = selectedOption,
            ) { onSelected(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // ok
@Composable
fun RadioButtonItem(
    radioColor: RadioButtonColors = RadioButtonDefaults.colors(
        selectedColor = MaterialTheme.colorScheme.secondary,
        unselectedColor = MaterialTheme.colorScheme.inverseSurface
    ),
    textColor: Color = MaterialTheme.colorScheme.inverseSurface,
    index: Int,
    item: String,
    selectedOption: Int,
    onSelected: (Int) -> Unit,
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
            colors = radioColor,
        )
        Text(text = item, color = textColor)
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Light Theme", uiMode = UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun RadioGroupPreview() {
    val list = listOf("Item 1", "Item 2", "Item 3")
    XmpTheme3 {
        RadioGroup(radioList = list, selectedOption = 0, onSelected = {})
    }
}
