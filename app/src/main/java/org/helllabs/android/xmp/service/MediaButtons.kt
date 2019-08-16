package org.helllabs.android.xmp.service

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

import org.helllabs.android.xmp.service.receiver.MediaButtonsReceiver
import org.helllabs.android.xmp.util.Log

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager

// for media buttons
// see http://android-developers.blogspot.com/2010/06/allowing-applications-to-play-nicer.html

internal class MediaButtons(context: Context) {
    private val audioManager: AudioManager
    private val mediaButtonsResponder: ComponentName

    init {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaButtonsResponder = ComponentName(context.packageName, MediaButtonsReceiver::class.java.name)
    }

    fun register() {
        try {
            if (registerMediaButtonEventReceiver == null) {
                return
            }
            registerMediaButtonEventReceiver!!.invoke(audioManager, mediaButtonsResponder)
        } catch (ite: InvocationTargetException) {
            // unpack original exception when possible
            val cause = ite.cause
            if (cause is RuntimeException) {
                throw cause
            } else if (cause is Error) {
                throw cause
            } else {
                // unexpected checked exception; wrap and re-throw
                throw RuntimeException(ite)    // NOPMD
            }
        } catch (ie: IllegalAccessException) {
            Log.e(TAG, "Unexpected $ie")
        }

    }

    fun unregister() {
        try {
            if (unregisterMediaButtonEventReceiver == null) {
                return
            }
            unregisterMediaButtonEventReceiver!!.invoke(audioManager, mediaButtonsResponder)
        } catch (ite: InvocationTargetException) {
            // unpack original exception when possible
            val cause = ite.cause
            if (cause is RuntimeException) {
                throw cause
            } else if (cause is Error) {
                throw cause
            } else {
                // unexpected checked exception; wrap and re-throw
                throw RuntimeException(ite)    // NOPMD
            }
        } catch (ie: IllegalAccessException) {
            Log.e(TAG, "Unexpected $ie")
        }

    }

    companion object {
        private val TAG = "MediaButtons"
        private var registerMediaButtonEventReceiver: Method? = null
        private var unregisterMediaButtonEventReceiver: Method? = null

        init {
            initializeRegistrationMethods()
        }

        private fun initializeRegistrationMethods() {
            try {
                if (registerMediaButtonEventReceiver == null) {        // NOPMD
                    registerMediaButtonEventReceiver = AudioManager::class.java
                            .getMethod("registerMediaButtonEventReceiver", ComponentName::class.java)
                }
                if (unregisterMediaButtonEventReceiver == null) {    // NOPMD
                    unregisterMediaButtonEventReceiver = AudioManager::class.java
                            .getMethod("unregisterMediaButtonEventReceiver", ComponentName::class.java)
                }
                /* success, this device will take advantage of better remote */
                /* control event handling */
            } catch (nsme: NoSuchMethodException) {
                /* failure, still using the legacy behavior, but this app */
                /* is future-proof! */
                Log.e(TAG, nsme.message!!)
            }

        }
    }
}
