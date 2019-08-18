package org.helllabs.android.xmp.util

import org.helllabs.android.xmp.R

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.widget.Toast


fun Activity.fatalError(message: String) {
    this.runOnUiThread {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle(R.string.error)
        alertDialog.setMessage(message)
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.exit)) { dialog, _ ->
            dialog.dismiss()
            finish()
        }
        alertDialog.show()
    }
}

fun Activity.error(message: String) {
    this.runOnUiThread {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle(R.string.error)
        alertDialog.setMessage(message)
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.dismiss)) { _, _ ->
            // Nothing
        }
        alertDialog.show()
    }
}

fun Activity.error(resId: Int) {
    this.error(getString(resId))
}

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.toast(resId: Int) {
    this.toast(getString(resId))
}

fun Activity.yesNoDialog(title: String, message: String, runnable: Runnable) {

    this.runOnUiThread {
        val listener = DialogInterface.OnClickListener { _, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                runnable.run()
            }
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.yes, listener)
                .setNegativeButton(R.string.no, listener)
                .show()
    }
}

