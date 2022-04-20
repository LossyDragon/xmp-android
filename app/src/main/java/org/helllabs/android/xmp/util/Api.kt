package org.helllabs.android.xmp.util

import android.os.Build

object Api {

    val isAtLeastM: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    val isAtLeastN: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    val isAtLeastO: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    val isAtLeastR: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    val isAtLeastS: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
