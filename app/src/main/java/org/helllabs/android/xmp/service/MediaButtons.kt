package org.helllabs.android.xmp.service

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import org.helllabs.android.xmp.service.receiver.MediaButtonsReceiver
import org.helllabs.android.xmp.util.Log.e

// for media buttons
// see http://android-developers.blogspot.com/2010/06/allowing-applications-to-play-nicer.html
internal class MediaButtons(context: Context) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val mediaButtonsResponder: ComponentName =
        ComponentName(context.packageName, MediaButtonsReceiver::class.java.name)

    fun register() {
        try {
            if (registerMediaButtonEventReceiver == null) {
                return
            }
            registerMediaButtonEventReceiver!!.invoke(audioManager, mediaButtonsResponder)
        } catch (ite: InvocationTargetException) {
            // unpack original exception when possible
            when (val cause = ite.cause) {
                is RuntimeException -> throw (cause as RuntimeException?)!!
                is Error -> throw (cause as Error?)!!
                else -> throw RuntimeException(ite)
            }
        } catch (ie: IllegalAccessException) {
            e(TAG, "Unexpected $ie")
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
            when (val cause = ite.cause) {
                is RuntimeException -> throw (cause as RuntimeException?)!!
                is Error -> throw (cause as Error?)!!
                else -> throw RuntimeException(ite)
            }
        } catch (ie: IllegalAccessException) {
            e(TAG, "Unexpected $ie")
        }
    }

    companion object {
        private const val TAG = "MediaButtons"
        private var registerMediaButtonEventReceiver: Method? = null
        private var unregisterMediaButtonEventReceiver: Method? = null

        init {
            initializeRegistrationMethods()
        }

        private fun initializeRegistrationMethods() {
            try {
                if (registerMediaButtonEventReceiver == null) {
                    registerMediaButtonEventReceiver = AudioManager::class.java
                        .getMethod("registerMediaButtonEventReceiver", ComponentName::class.java)
                }
                if (unregisterMediaButtonEventReceiver == null) {
                    unregisterMediaButtonEventReceiver = AudioManager::class.java
                        .getMethod("unregisterMediaButtonEventReceiver", ComponentName::class.java)
                }
                /* success, this device will take advantage of better remote */
                /* control event handling */
            } catch (nsme: NoSuchMethodException) {
                /* failure, still using the legacy behavior, but this app */
                /* is future-proof! */
                e(TAG, nsme.message!!)
            }
        }
    }
}
