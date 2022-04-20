package org.helllabs.android.xmp.util

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

inline fun <reified T : Context> T.toast(message: String) =
    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()

inline fun <reified T : Context> T.toast(@StringRes resId: Int) =
    Toast.makeText(applicationContext, this.getString(resId), Toast.LENGTH_SHORT).show()
