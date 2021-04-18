package org.helllabs.android.xmp.presentation.ui.playlistEdit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.navigationBarsWithImePadding
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.presentation.components.AppBar
import org.helllabs.android.xmp.presentation.theme.AppTheme
import org.helllabs.android.xmp.presentation.theme.darkPrimary
import org.helllabs.android.xmp.presentation.theme.systemDarkTheme
import org.helllabs.android.xmp.presentation.theme.white
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.toast
import org.helllabs.android.xmp.util.yesNoDialog

class PlaylistEdit : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val isEditing = intent.hasExtra(EXTRA_ID)
        val intentName = intent?.getStringExtra(EXTRA_NAME).orEmpty()
        val intentComment = intent?.getStringExtra(EXTRA_COMMENT).orEmpty()

        logD("onCreate")
        setContent {
            val context = LocalContext.current
            PlaylistEditLayout(
                isEditing = isEditing,
                intentName = intentName,
                intentComment = intentComment,
                onBack = { onBackPressed() },
                onEdit = { name, comment ->
                    if (name.trim().isBlank()) {
                        toast(R.string.error_playlist_name)
                        return@PlaylistEditLayout
                    }

                    Intent().apply {
                        putExtra(EXTRA_NAME, name)
                        putExtra(EXTRA_COMMENT, comment)
                        if (isEditing) {
                            putExtra(EXTRA_OLD_NAME, intent.getStringExtra(EXTRA_NAME))
                            putExtra(EXTRA_ID, RESULT_EDIT_PLAYLIST)
                        } else {
                            putExtra(EXTRA_ID, RESULT_NEW_PLAYLIST)
                        }
                    }.also {
                        setResult(RESULT_OK, it)
                        finish()
                    }
                },
                onDelete = {
                    context.yesNoDialog(
                        owner = this,
                        title = R.string.dialog_delete_playlist,
                        message = getString(R.string.dialog_delete_playlist_message, intentName),
                        confirmText = R.string.menu_delete,
                        dismissText = R.string.cancel,
                        onConfirm = {
                            Intent().apply {
                                putExtra(EXTRA_ID, RESULT_DELETE_PLAYLIST)
                                putExtra(EXTRA_NAME, intent.getStringExtra(EXTRA_NAME))
                                putExtra(EXTRA_COMMENT, intent.getStringExtra(EXTRA_COMMENT))
                            }.also { intent ->
                                setResult(RESULT_OK, intent)
                                finish()
                            }
                        }
                    )
                }
            )
        }
    }

    companion object {
        const val RESULT_NEW_PLAYLIST = 0
        const val RESULT_EDIT_PLAYLIST = 1
        const val RESULT_DELETE_PLAYLIST = 2

        const val EXTRA_ID = "org.helllabs.android.xmp.ui.browser.EXTRA_ID"
        const val EXTRA_NAME = "org.helllabs.android.xmp.ui.browser.EXTRA_NAME"
        const val EXTRA_OLD_NAME = "org.helllabs.android.xmp.ui.browser.EXTRA_OLD_NAME"
        const val EXTRA_COMMENT = "org.helllabs.android.xmp.ui.browser.EXTRA_COMMENT"
        const val EXTRA_TYPE = "org.helllabs.android.xmp.ui.browser.EXTRA_TYPE"
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlaylistEditLayout(
    isDarkTheme: Boolean = systemDarkTheme(),
    isEditing: Boolean = false,
    intentName: String,
    intentComment: String,
    onBack: () -> Unit,
    onEdit: (name: String, comment: String) -> Unit,
    onDelete: () -> Unit,
) {
    val appTitle = if (isEditing) {
        R.string.title_edit_playlist
    } else {
        R.string.menu_new_playlist
    }
    var name by rememberSaveable { mutableStateOf(intentName) }
    var comment by rememberSaveable { mutableStateOf(intentComment) }
    val addText = if (isEditing) R.string.button_playlist_update else R.string.button_playlist_add

    AppTheme(
        isDarkTheme = isDarkTheme
    ) {
        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(id = appTitle),
                    navIconClick = { onBack() },
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .navigationBarsWithImePadding()
            ) {
                val focusManager = LocalFocusManager.current

                // More error fields to be added:
                // See: https://stackoverflow.com/q/65642533/13225929
                // https://issuetracker.google.com/issues/182142737
                OutlinedTextField(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    isError = name.isEmpty(),
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
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp),
                    text = if (name.isEmpty()) helperText else "",
                    fontSize = 10.sp
                )
                // More error fields to be added:
                // See: https://stackoverflow.com/q/65642533/13225929
                // https://issuetracker.google.com/issues/182142737
                OutlinedTextField(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 12.dp)
                        .fillMaxWidth(),
                    value = comment,
                    onValueChange = { comment = it },
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onEdit(name, comment)
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
                Button(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 12.dp)
                        .fillMaxWidth(),
                    onClick = {
                        onEdit(name, comment)
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = darkPrimary)
                ) {
                    Text(
                        text = stringResource(id = addText),
                        color = white
                    )
                }
                if (isEditing) {
                    Button(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 12.dp)
                            .fillMaxWidth(),
                        onClick = { onDelete() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = darkPrimary)
                    ) {
                        Text(
                            text = stringResource(id = R.string.button_playlist_delete, intentName),
                            color = white
                        )
                    }
                }
            }
        }
    }
}

/************
 * Previews *
 ************/

@Preview
@Composable
fun PlaylistAddEditPreview() {
    PlaylistEditLayout(
        isDarkTheme = false,
        isEditing = true,
        intentName = "Name",
        intentComment = "Comment",
        onBack = {},
        onEdit = { _, _ -> },
        onDelete = {},
    )
}

@Preview
@Composable
fun PlaylistAddEditPreviewDark() {
    PlaylistEditLayout(
        isDarkTheme = true,
        isEditing = true,
        intentName = "Name",
        intentComment = "Comment",
        onBack = {},
        onEdit = { _, _ -> },
        onDelete = {},
    )
}
