package org.helllabs.android.xmp.util

import android.content.Context
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.LifecycleOwner
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.preferences.PrefManager

inline fun <reified T : Context> T.toast(message: String) =
    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()

inline fun <reified T : Context> T.toast(@StringRes resId: Int) =
    Toast.makeText(applicationContext, this.getString(resId), Toast.LENGTH_SHORT).show()

fun Context.dialogMessage(
    lifecycleOwner: LifecycleOwner,
    @StringRes title: Int = R.string.error,
    message: String,
    @StringRes positiveButtonText: Int = R.string.ok,
    block: (() -> Unit)? = { }
) {
    MaterialDialog(this).show {
        lifecycleOwner(lifecycleOwner)
        title(title)
        message(text = message)
        positiveButton(positiveButtonText) { block?.invoke() }
    }
}

fun Context.yesNoDialog(
    lifecycleOwner: LifecycleOwner,
    title: String,
    message: String,
    @StringRes positiveButton: Int = R.string.yes,
    @StringRes negativeButton: Int = R.string.no,
    onNegativeButton: (() -> Unit)? = null,
    onPositiveButton: () -> Unit,
) {
    MaterialDialog(this).show {
        lifecycleOwner(lifecycleOwner)
        title(text = title)
        message(text = message)
        positiveButton(positiveButton) { onPositiveButton() }
        negativeButton(negativeButton) { onNegativeButton?.invoke() }
    }
}

@Composable
fun DialogShowChangelog(
    onDismiss: () -> Unit
) {
    if (PrefManager.changelogVersion < BuildConfig.VERSION_CODE) {
        // TODO show dialog
    }

    val dismiss = {
        PrefManager.changelogVersion = BuildConfig.VERSION_CODE
        onDismiss()
    }

    Dialog(onDismissRequest = dismiss) {
        Surface(
            modifier = Modifier
                .fillMaxHeight(.90f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.LightGray
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    modifier = Modifier.align(Alignment.TopCenter),
                    text = "top"
                )
                androidx.compose.material3.Text("center")
                androidx.compose.material3.Text(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    text = "bottom"
                )
            }
        }
    }

//    AlertDialog(
//        title = {
//            androidx.compose.material3.Text(text = "Changelog")
//        },
//        text = {
//            val scrollState = rememberScrollState()
//            val title = stringResource(R.string.changelog_title, BuildConfig.VERSION_NAME)
//            Column(
//                modifier = Modifier
//                    .fillMaxSize(.75f)
//                    .verticalScroll(scrollState),
//                horizontalAlignment = Alignment.CenterHorizontally,
//            ) {
//                androidx.compose.material3.Text(title)
//                Spacer(modifier = Modifier.height(8.dp))
//                androidx.compose.material3.Text(stringResource(id = R.string.changelog_text))
//            }
//        },
//        onDismissRequest = dismiss,
//        confirmButton = {
//            TextButton(onClick = dismiss) {
//                androidx.compose.material3.Text(text = stringResource(id = R.string.ok))
//            }
//        }
//    )
}

fun Context.showChangeLog(lifecycleOwner: LifecycleOwner, block: () -> Unit) {
    if (PrefManager.changelogVersion < BuildConfig.VERSION_CODE) {
        MaterialDialog(this).show {
            lifecycleOwner(lifecycleOwner)
            customView(R.layout.layout_changelog)
            getCustomView().findViewById<TextView>(R.id.changelog_version_title).apply {
                text = getString(R.string.changelog_title, BuildConfig.VERSION_NAME)
            }
            cancelOnTouchOutside(false)
            title(text = "")
            positiveButton(text = "Dismiss") {
                PrefManager.changelogVersion = BuildConfig.VERSION_CODE
                block()
            }
        }
    } else {
        block()
    }
}
