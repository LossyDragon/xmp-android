package org.helllabs.android.xmp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.util.toast

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NewPlaylistLayout(
    name: String,
    comment: String,
    onName: (value: String) -> Unit,
    onComment: (value: String) -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val focusManager = LocalFocusManager.current
        val focusRequester = FocusRequester()
        val keyboard = LocalSoftwareKeyboardController.current

        val checkName: Boolean = name.trim().isBlank()

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            value = name,
            onValueChange = { onName(it) },
            isError = name.isEmpty(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Text
            ),
            maxLines = 1,
            label = { Text(stringResource(id = R.string.hint_playlist_name)) },
        )
        val helperText = stringResource(id = R.string.playlist_edit_helper_text)
        Row {
            AnimatedVisibility(
                visible = name.isEmpty(),
                enter = fadeIn(initialAlpha = 0.4f),
                exit = fadeOut(animationSpec = tween(durationMillis = 250))
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    text = helperText,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = comment,
            onValueChange = { onComment(it) },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    // Check if name is empty.
                    if (checkName) {
                        context.toast(R.string.error_playlist_name)
                        focusManager.moveFocus(FocusDirection.Up)
                        return@KeyboardActions
                    }
                    focusManager.clearFocus()
                }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),
            maxLines = 3,
            label = { Text(stringResource(id = R.string.hint_playlist_comment)) },
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Request focus and show the keyboard.
        // Showing the keyboard is very sporadic
        LaunchedEffect(true) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
}
