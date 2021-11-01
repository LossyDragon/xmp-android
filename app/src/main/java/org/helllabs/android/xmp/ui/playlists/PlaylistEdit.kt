package org.helllabs.android.xmp.ui.playlists

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.navigationBarsWithImePadding
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.components.AppBar
import org.helllabs.android.xmp.ui.theme.XmpTheme
import org.helllabs.android.xmp.ui.theme.darkPrimary
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.toast
import org.helllabs.android.xmp.util.yesNoDialog

const val PLAYLIST_EDIT_ID = "org.helllabs.android.xmp.ui.playlistMenu.PLAYLIST_EDIT_ID"
const val PLAYLIST_EDIT_NAME = "org.helllabs.android.xmp.ui.playlistMenu.PLAYLIST_EDIT_NAME"
const val PLAYLIST_EDIT_OLD_NAME = "org.helllabs.android.xmp.ui.playlistMenu.PLAYLIST_EDIT_OLD_NAME"
const val PLAYLIST_EDIT_COMMENT = "org.helllabs.android.xmp.ui.playlistMenu.PLAYLIST_EDIT_COMMENT"

enum class EditState(val value: Int) {
    RESULT_NEW_PLAYLIST(0),
    RESULT_EDIT_PLAYLIST(1),
    RESULT_DELETE_PLAYLIST(2),
}

class PlaylistEdit : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        logD("onCreate")
        setContent {
            PlaylistEditScreen(
                intent = intent,
                onBack = { onBackPressed() },
            )
        }
    }
}

@Composable
private fun PlaylistEditScreen(
    intent: Intent?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isEditing = intent?.hasExtra(PLAYLIST_EDIT_ID) ?: false
    val intentName = intent?.getStringExtra(PLAYLIST_EDIT_NAME).orEmpty()
    val intentComment = intent?.getStringExtra(PLAYLIST_EDIT_COMMENT).orEmpty()
    val onEdit: (name: String, comment: String) -> Unit = { name, comment ->
        val playlistData = Intent().apply {
            putExtra(PLAYLIST_EDIT_NAME, name)
            putExtra(PLAYLIST_EDIT_COMMENT, comment)
            if (isEditing) {
                putExtra(PLAYLIST_EDIT_OLD_NAME, intentName)
                putExtra(PLAYLIST_EDIT_ID, EditState.RESULT_EDIT_PLAYLIST.value)
            } else {
                putExtra(PLAYLIST_EDIT_ID, EditState.RESULT_NEW_PLAYLIST.value)
            }
        }

        (context as Activity).setResult(RESULT_OK, playlistData)
        context.finish()
    }
    val onDelete = {
        context.yesNoDialog(
            lifecycleOwner = lifecycleOwner,
            title = context.getString(R.string.dialog_delete_playlist),
            message = context.getString(R.string.dialog_delete_playlist_message, intentName),
            positiveButton = R.string.menu_delete,
            negativeButton = R.string.cancel,
            onPositiveButton = {
                Intent().apply {
                    putExtra(PLAYLIST_EDIT_ID, EditState.RESULT_DELETE_PLAYLIST.value)
                    putExtra(PLAYLIST_EDIT_NAME, intentName)
                    putExtra(PLAYLIST_EDIT_COMMENT, intentComment)
                }.also { intent ->
                    (context as Activity).setResult(RESULT_OK, intent)
                    context.finish()
                    context.overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                }
            }
        )
    }

    PlaylistEditContent(
        intentName = intentName,
        intentComment = intentComment,
        isEditing = isEditing,
        onBack = { onBack() },
        onEdit = { name, comment -> onEdit(name, comment) },
        onDelete = { onDelete() },
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PlaylistEditContent(
    appTitle: String = stringResource(id = R.string.menu_new_playlist),
    intentName: String,
    intentComment: String,
    isEditing: Boolean,
    onBack: () -> Unit,
    onEdit: (name: String, comment: String) -> Unit,
    onDelete: () -> Unit,
) {
    var appBarTitle = appTitle
    if (isEditing)
        appBarTitle = stringResource(id = R.string.title_edit_playlist)

    var name by rememberSaveable { mutableStateOf(intentName) }
    var comment by rememberSaveable { mutableStateOf(intentComment) }
    val addText = if (isEditing) R.string.button_playlist_update else R.string.button_playlist_add
    val checkName: Boolean = name.trim().isBlank()

    XmpTheme {
        Scaffold(
            topBar = {
                AppBar(
                    title = appBarTitle,
                    navIconClick = { onBack() },
                )
            }
        ) {
            val context = LocalContext.current
            val focusManager = LocalFocusManager.current
            val focusRequester = FocusRequester()
            val keyboard = LocalSoftwareKeyboardController.current

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .navigationBarsWithImePadding()
            ) {
                // More error fields to be added:
                // See: https://stackoverflow.com/q/65642533/13225929
                // https://issuetracker.google.com/issues/182142737
                OutlinedTextField(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
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
                            // Check if name is empty.
                            if (checkName) {
                                context.toast(R.string.error_playlist_name)
                                focusManager.moveFocus(FocusDirection.Up)
                                return@KeyboardActions
                            }
                            focusManager.clearFocus()
                            onEdit(name, comment)
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
                        // Check if name is empty.
                        if (checkName) {
                            context.toast(R.string.error_playlist_name)
                            return@Button
                        }
                        focusManager.clearFocus()
                        onEdit(name, comment)
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = darkPrimary)
                ) {
                    Text(
                        text = stringResource(id = addText),
                        color = Color.White
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
                            color = Color.White
                        )
                    }
                }
            }

            // Request focus and show the keyboard.
            // Showing the keyboard is very sporadic
            DisposableEffect(Unit) {
                focusRequester.requestFocus()
                keyboard?.show()
                onDispose { }
            }
        }
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlaylistEditPreview() {
    PlaylistEditContent(
        intentName = "Playlist Name",
        intentComment = "Playlist Comment",
        isEditing = true,
        onBack = { },
        onEdit = { _, _ -> },
        onDelete = {}
    )
}
