package org.helllabs.android.xmp.player

import android.content.Context
import android.content.res.Configuration

class ScreenSizeHelper {
    fun getScreenSize(context: Context): Int {
        return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
    }
}
