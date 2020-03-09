package org.helllabs.android.xmp.extension

import android.os.Build

fun isAtLeastP() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
fun isAtLeastL() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
fun isAtLeastN() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
fun isAtLeastO() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
fun isAtMostN() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N
