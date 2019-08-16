package org.helllabs.android.xmp.util

import org.helllabs.android.xmp.R

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.widget.Toast

object Message {

    fun fatalError(activity: Activity, message: String) {

        activity.runOnUiThread {
            val alertDialog = AlertDialog.Builder(activity).create()
            alertDialog.setTitle(R.string.error)
            alertDialog.setMessage(message)
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, activity.getString(R.string.exit)) { dialog, which ->
                dialog.dismiss()
                activity.finish()
            }
            alertDialog.show()
        }
    }

    fun error(activity: Activity, message: String) {

        activity.runOnUiThread {
            val alertDialog = AlertDialog.Builder(activity).create()
            alertDialog.setTitle(R.string.error)
            alertDialog.setMessage(message)
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, activity.getString(R.string.dismiss)) { dialog, which ->
                //
            }
            alertDialog.show()
        }
    }

    fun error(activity: Activity, resId: Int) {
        error(activity, activity.getString(resId))
    }

    fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun toast(context: Context, resId: Int) {
        toast(context, context.getString(resId))
    }

    fun yesNoDialog(activity: Activity, title: String, message: String, runnable: Runnable) {

        activity.runOnUiThread {
            val listener = DialogInterface.OnClickListener { dialog, which ->
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    runnable.run()
                }
            }

            val builder = AlertDialog.Builder(activity)
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(R.string.yes, listener)
                    .setNegativeButton(R.string.no, listener)
                    .show()
        }
    }
}
