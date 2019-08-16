/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.helllabs.android.xmp.service.utils

import android.media.AudioManager
import android.util.Log

import java.lang.reflect.Method

/**
 * Contains methods to handle registering/unregistering remote control clients.  These methods only
 * run on ICS devices.  On previous devices, all methods are no-ops.
 */
object RemoteControlHelper {
    private val TAG = "RemoteControlHelper"

    private var sHasRemoteControlAPIs = false

    private var sRegisterRemoteControlClientMethod: Method? = null
    private var sUnregisterRemoteControlClientMethod: Method? = null

    init {
        try {
            val classLoader = RemoteControlHelper::class.java.classLoader
            val sRemoteControlClientClass = RemoteControlClientCompat.getActualRemoteControlClientClass(classLoader!!)
            sRegisterRemoteControlClientMethod = AudioManager::class.java.getMethod(
                    "registerRemoteControlClient", sRemoteControlClientClass)
            sUnregisterRemoteControlClientMethod = AudioManager::class.java.getMethod(
                    "unregisterRemoteControlClient", sRemoteControlClientClass)
            sHasRemoteControlAPIs = true
        } catch (e: ClassNotFoundException) {
            // Silently fail when running on an OS before ICS.
        } catch (e: NoSuchMethodException) {
            // Silently fail when running on an OS before ICS.
        } catch (e: IllegalArgumentException) {
            // Silently fail when running on an OS before ICS.
        } catch (e: SecurityException) {
            // Silently fail when running on an OS before ICS.
        }

    }

    fun registerRemoteControlClient(audioManager: AudioManager,
                                    remoteControlClient: RemoteControlClientCompat) {
        if (!sHasRemoteControlAPIs) {
            return
        }

        try {
            sRegisterRemoteControlClientMethod!!.invoke(audioManager,
                    remoteControlClient.actualRemoteControlClientObject)
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }

    }


    fun unregisterRemoteControlClient(audioManager: AudioManager,
                                      remoteControlClient: RemoteControlClientCompat) {
        if (!sHasRemoteControlAPIs) {
            return
        }

        try {
            sUnregisterRemoteControlClientMethod!!.invoke(audioManager,
                    remoteControlClient.actualRemoteControlClientObject)
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }

    }
}
