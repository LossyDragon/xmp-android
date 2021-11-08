package org.helllabs.android.xmp.ui.playlists

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.*
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.components.XmpAppBar3
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.ui.theme.darkAccent
import org.helllabs.android.xmp.util.DialogMessage
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.toast

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
            ProvideWindowInsets {
                PlaylistEditScreen(
                    intent = intent,
                    onBack = { onBackPressed() },
                )
            }
        }
    }
}

@Composable
private fun PlaylistEditScreen(
    intent: Intent?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isEditing = intent?.hasExtra(PLAYLIST_EDIT_ID) ?: false
    val intentName = intent?.getStringExtra(PLAYLIST_EDIT_NAME).orEmpty()
    val intentComment = intent?.getStringExtra(PLAYLIST_EDIT_COMMENT).orEmpty()

    PlaylistEditContent(
        intentName = intentName,
        intentComment = intentComment,
        isEditing = isEditing,
        onBack = { onBack() },
        onEdit = { name, comment ->
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
        },
    )
}

@OptIn(
    ExperimentalAnimatedInsets::class,
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class
)
@Composable
private fun PlaylistEditContent(
    @StringRes appTitle: Int = R.string.menu_new_playlist,
    intentName: String,
    intentComment: String,
    isEditing: Boolean,
    onBack: () -> Unit,
    onEdit: (name: String, comment: String) -> Unit,
) {
    val context = LocalContext.current

    val deleteState = rememberMaterialDialogState()
    DialogMessage(
        dialogState = deleteState,
        title = R.string.dialog_delete_playlist,
        messageText = stringResource(id = R.string.dialog_delete_playlist_message, intentName),
        positiveButtonText = R.string.menu_delete,
        negativeButtonText = R.string.cancel,
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
        },
        onDismiss = { deleteState.hide() }
    )

    XmpTheme3 {
        val scrollState = rememberScrollState()
        val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

        Scaffold(
            topBar = {
                // Top App Bar
                val rotation = LocalConfiguration.current.orientation
                val appBarModifier =
                    if (rotation == Configuration.ORIENTATION_PORTRAIT) Modifier.statusBarsPadding()
                    else Modifier.systemBarsPadding()

                val title = if (isEditing) R.string.title_edit_playlist else appTitle
                XmpAppBar3(
                    modifier = appBarModifier,
                    scrollBehavior = scrollBehavior,
                    titleText = stringResource(id = title),
                    onNavIconPressed = onBack,
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp)
                    .navigationBarsWithImePadding()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                var name by rememberSaveable { mutableStateOf(intentName) }
                var comment by rememberSaveable { mutableStateOf(intentComment) }
                val focusManager = LocalFocusManager.current
                val focusRequester = FocusRequester()
                val keyboard = LocalSoftwareKeyboardController.current

                val checkName: Boolean = name.trim().isBlank()
                val addText =
                    if (isEditing) R.string.button_playlist_update
                    else R.string.button_playlist_add

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    value = name,
                    onValueChange = { name = it },
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
                    onValueChange = { comment = it },
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
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    modifier = Modifier
                        .fillMaxWidth(.85f),
                    colors = ButtonDefaults.buttonColors(containerColor = darkAccent),
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
                ) {
                    Text(
                        text = stringResource(id = addText),
                        color = Color.White,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (isEditing) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(.85f),
                        colors = ButtonDefaults.buttonColors(containerColor = darkAccent),
                        onClick = { deleteState.show() },
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.button_playlist_delete, intentName
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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
    )
}
