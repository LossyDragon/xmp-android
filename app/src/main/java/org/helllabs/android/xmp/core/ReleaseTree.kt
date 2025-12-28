package org.helllabs.android.xmp.core

import android.util.Log
import timber.log.Timber

class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.DEBUG) {
            return
        }

        Log.println(priority, "Xmp Mod Player", message)
    }
}
