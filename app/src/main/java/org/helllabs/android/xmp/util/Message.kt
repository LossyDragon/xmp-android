package org.helllabs.android.xmp.util

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vanpra.composematerialdialogs.*
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R

inline fun <reified T : Context> T.toast(message: String) =
    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()

inline fun <reified T : Context> T.toast(@StringRes resId: Int) =
    Toast.makeText(applicationContext, this.getString(resId), Toast.LENGTH_SHORT).show()

@Composable
fun DialogMessage(
    dialogState: MaterialDialogState,
    @StringRes title: Int,
    @StringRes message: Int? = null,
    messageText: String? = null,
    @StringRes positiveButtonText: Int,
    @StringRes negativeButtonText: Int? = null,
    onPositiveButton: () -> Unit,
    onNegativeButton: (() -> Unit)? = null,
    onDismiss: () -> Unit = {},
) {
    MaterialDialog(
        shape = RoundedCornerShape(8.dp),
        dialogState = dialogState,
        onCloseRequest = { onDismiss() },
        buttons = {
            positiveButton(res = positiveButtonText) {
                onPositiveButton()
            }
            onNegativeButton?.let {
                negativeButton(res = negativeButtonText) {
                    onNegativeButton()
                }
            }
        }
    ) {
        title(res = title)
        if (message != null) message(res = message) else message(text = messageText)
    }
}

@Composable
fun DialogShowChangelog(
    dialogState: MaterialDialogState,
    block: () -> Unit,
) {
    MaterialDialog(
        shape = RoundedCornerShape(8.dp),
        dialogState = dialogState,
        onCloseRequest = {},
        buttons = {
            positiveButton(text = "Dismiss") {
                block()
            }
        }
    ) {
        title(res = R.string.changelog)
        customView {
            val scrollState = rememberScrollState()
            val changeLogBanner = stringResource(R.string.changelog_title, BuildConfig.VERSION_NAME)
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                Text(changeLogBanner)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(id = R.string.changelog_text))
            }
        }
    }

    if (PrefManager.changelogVersion < BuildConfig.VERSION_CODE) {
        SideEffect {
            dialogState.show()
        }
    } else {
        block()
    }
}
