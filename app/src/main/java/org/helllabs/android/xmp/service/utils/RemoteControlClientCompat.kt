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

import android.app.PendingIntent
import android.graphics.Bitmap
import android.util.Log
import java.lang.reflect.Method

/**
 * RemoteControlClient enables exposing information meant to be consumed by remote controls capable
 * of displaying metadata, artwork and media transport control buttons. A remote control client
 * object is associated with a media button event receiver. This event receiver must have been
 * previously registered with
 * [android.media.AudioManager.registerMediaButtonEventReceiver]
 * before the RemoteControlClient can be registered through
 * [android.media.AudioManager.registerRemoteControlClient].
 */
class RemoteControlClientCompat(pendingIntent: PendingIntent) {

    var actualRemoteControlClientObject: Any? = null
        private set

    init {
        //if (!sHasRemoteControlAPIs) {
        //    return
        //}
        try {
            actualRemoteControlClientObject = sRemoteControlClientClass!!.getConstructor(PendingIntent::class.java)
                    .newInstance(pendingIntent)
        } catch (e: Exception) {
            throw RuntimeException(e)    // NOPMD
        }
    }

    //constructor(pendingIntent: PendingIntent, looper: Looper) {
    //    if (!sHasRemoteControlAPIs) {
    //        return
    //    }
    //
    //    try {
    //        actualRemoteControlClientObject = sRemoteControlClientClass!!.getConstructor(PendingIntent::class.java, Looper::class.java)
    //                .newInstance(pendingIntent, looper)
    //    } catch (e: Exception) {
    //        Log.e(TAG, "Error creating new instance of " + sRemoteControlClientClass!!.name, e)
    //    }
    //
    //}

    /**
     * Class used to modify metadata in a [android.media.RemoteControlClient] object. Use
     * [android.media.RemoteControlClient.editMetadata] to create an instance of an
     * editor, on which you set the metadata for the RemoteControlClient instance. Once all the
     * information has been set, use [.apply] to make it the new metadata that should be
     * displayed for the associated client. Once the metadata has been "applied", you cannot reuse
     * this instance of the MetadataEditor.
     */
    class MetadataEditorCompat internal constructor(private val mActualMetadataEditor: Any?) {

        private var mPutStringMethod: Method? = null
        private var mPutBitmapMethod: Method? = null
        private var mPutLongMethod: Method? = null
        private var mClearMethod: Method? = null
        private var mApplyMethod: Method? = null

        init {
            if (sHasRemoteControlAPIs && mActualMetadataEditor == null) {
                throw IllegalArgumentException("Remote Control API's exist, " + "should not be given a null MetadataEditor")
            }
            if (sHasRemoteControlAPIs) {
                val metadataEditorClass = mActualMetadataEditor!!.javaClass

                try {
                    mPutStringMethod = metadataEditorClass.getMethod("putString", Int::class.javaPrimitiveType, String::class.java)
                    mPutBitmapMethod = metadataEditorClass.getMethod("putBitmap", Int::class.javaPrimitiveType, Bitmap::class.java)
                    mPutLongMethod = metadataEditorClass.getMethod("putLong", Int::class.javaPrimitiveType, Long::class.javaPrimitiveType)
                    mClearMethod = metadataEditorClass.getMethod("clear")
                    mApplyMethod = metadataEditorClass.getMethod("apply")
                } catch (e: Exception) {
                    throw RuntimeException(e.message, e)    // NOPMD
                }

            }
        }

        /**
         * Adds textual information to be displayed.
         * Note that none of the information added after [.apply] has been called,
         * will be displayed.
         * @param key The identifier of a the metadata field to set. Valid values are
         * [android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM],
         * [android.media.MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST],
         * [android.media.MediaMetadataRetriever.METADATA_KEY_TITLE],
         * [android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST],
         * [android.media.MediaMetadataRetriever.METADATA_KEY_AUTHOR],
         * [android.media.MediaMetadataRetriever.METADATA_KEY_COMPILATION],
         * [android.media.MediaMetadataRetriever.METADATA_KEY_COMPOSER],
         * [android.media.MediaMetadataRetriever.METADATA_KEY_DATE],
         * [android.media.MediaMetadataRetriever.METADATA_KEY_GENRE],
         * [android.media.MediaMetadataRetriever.METADATA_KEY_TITLE],
         * [android.media.MediaMetadataRetriever.METADATA_KEY_WRITER].
         * @param value The text for the given key, or `null` to signify there is no valid
         * information for the field.
         * @return Returns a reference to the same MetadataEditor object, so you can chain put
         * calls together.
         */
        fun putString(key: Int, value: String): MetadataEditorCompat {
            if (sHasRemoteControlAPIs) {
                try {
                    mPutStringMethod!!.invoke(mActualMetadataEditor, key, value)
                } catch (e: Exception) {
                    throw RuntimeException(e.message, e)    // NOPMD
                }

            }
            return this
        }

        ///**
        // * Sets the album / artwork picture to be displayed on the remote control.
        // * @param key the identifier of the bitmap to set. The only valid value is
        // * [.METADATA_KEY_ARTWORK]
        // * @param bitmap The bitmap for the artwork, or null if there isn't any.
        // * @return Returns a reference to the same MetadataEditor object, so you can chain put
        // * calls together.
        // * @throws IllegalArgumentException
        // * @see android.graphics.Bitmap
        // */
        //fun putBitmap(key: Int, bitmap: Bitmap): MetadataEditorCompat {
        //    if (sHasRemoteControlAPIs) {
        //        try {
        //            mPutBitmapMethod!!.invoke(mActualMetadataEditor, key, bitmap)
        //        } catch (e: Exception) {
        //            throw RuntimeException(e.message, e)    // NOPMD
        //        }
        //
        //    }
        //    return this
        //}

        /**
         * Adds numerical information to be displayed.
         * Note that none of the information added after [.apply] has been called,
         * will be displayed.
         * @param key the identifier of a the metadata field to set. Valid values are
         * [android.media.MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER],
         * [android.media.MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER],
         * [android.media.MediaMetadataRetriever.METADATA_KEY_DURATION] (with a value
         * expressed in milliseconds),
         * [android.media.MediaMetadataRetriever.METADATA_KEY_YEAR].
         * @param value The long value for the given key
         * @return Returns a reference to the same MetadataEditor object, so you can chain put
         * calls together.
         * @throws IllegalArgumentException
         */
        fun putLong(key: Int, value: Long): MetadataEditorCompat {
            if (sHasRemoteControlAPIs) {
                try {
                    mPutLongMethod!!.invoke(mActualMetadataEditor, key, value)
                } catch (e: Exception) {
                    throw RuntimeException(e.message, e)    // NOPMD
                }

            }
            return this
        }

        /**
         * Clears all the metadata that has been set since the MetadataEditor instance was
         * created with [android.media.RemoteControlClient.editMetadata].
         */
        fun clear() {
            if (sHasRemoteControlAPIs) {
                try {
                    mClearMethod!!.invoke(mActualMetadataEditor)
                } catch (e: Exception) {
                    throw RuntimeException(e.message, e)    // NOPMD
                }

            }
        }

        /**
         * Associates all the metadata that has been set since the MetadataEditor instance was
         * created with [android.media.RemoteControlClient.editMetadata], or since
         * [.clear] was called, with the RemoteControlClient. Once "applied", this
         * MetadataEditor cannot be reused to edit the RemoteControlClient's metadata.
         */
        fun apply() {
            if (sHasRemoteControlAPIs) {
                try {
                    mApplyMethod!!.invoke(mActualMetadataEditor)
                } catch (e: Exception) {
                    throw RuntimeException(e.message, e)    // NOPMD
                }

            }
        }

        companion object {

            /**
             * The metadata key for the content artwork / album art.
             */
            //const val METADATA_KEY_ARTWORK = 100
        }
    }

    /**
     * Creates a [android.media.RemoteControlClient.MetadataEditor].
     * @param startEmpty Set to false if you want the MetadataEditor to contain the metadata that
     * was previously applied to the RemoteControlClient, or true if it is to be created empty.
     * @return a new MetadataEditor instance.
     */
    fun editMetadata(startEmpty: Boolean): MetadataEditorCompat {
        val metadataEditor: Any?
        if (sHasRemoteControlAPIs) {
            try {
                metadataEditor = sRCCEditMetadataMethod!!.invoke(actualRemoteControlClientObject,
                        startEmpty)
            } catch (e: Exception) {
                throw RuntimeException(e)    // NOPMD
            }

        } else {
            metadataEditor = null
        }
        return MetadataEditorCompat(metadataEditor)
    }

    /**
     * Sets the current playback state.
     * @param state The current playback state, one of the following values:
     * [android.media.RemoteControlClient.PLAYSTATE_STOPPED],
     * [android.media.RemoteControlClient.PLAYSTATE_PAUSED],
     * [android.media.RemoteControlClient.PLAYSTATE_PLAYING],
     * [android.media.RemoteControlClient.PLAYSTATE_FAST_FORWARDING],
     * [android.media.RemoteControlClient.PLAYSTATE_REWINDING],
     * [android.media.RemoteControlClient.PLAYSTATE_SKIPPING_FORWARDS],
     * [android.media.RemoteControlClient.PLAYSTATE_SKIPPING_BACKWARDS],
     * [android.media.RemoteControlClient.PLAYSTATE_BUFFERING],
     * [android.media.RemoteControlClient.PLAYSTATE_ERROR].
     */
    fun setPlaybackState(state: Int) {
        if (sHasRemoteControlAPIs) {
            try {
                sRCCSetPlayStateMethod!!.invoke(actualRemoteControlClientObject, state)
            } catch (e: Exception) {
                throw RuntimeException(e)    // NOPMD
            }

        }
    }

    /**
     * Sets the flags for the media transport control buttons that this client supports.
     * @param transportControlFlags A combination of the following flags:
     * [android.media.RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS],
     * [android.media.RemoteControlClient.FLAG_KEY_MEDIA_REWIND],
     * [android.media.RemoteControlClient.FLAG_KEY_MEDIA_PLAY],
     * [android.media.RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE],
     * [android.media.RemoteControlClient.FLAG_KEY_MEDIA_PAUSE],
     * [android.media.RemoteControlClient.FLAG_KEY_MEDIA_STOP],
     * [android.media.RemoteControlClient.FLAG_KEY_MEDIA_FAST_FORWARD],
     * [android.media.RemoteControlClient.FLAG_KEY_MEDIA_NEXT]
     */
    fun setTransportControlFlags(transportControlFlags: Int) {
        if (sHasRemoteControlAPIs) {
            try {
                sRCCSetTransportControlFlags!!.invoke(actualRemoteControlClientObject,
                        transportControlFlags)
            } catch (e: Exception) {
                throw RuntimeException(e)    // NOPMD
            }

        }
    }

    companion object {

        private const val TAG = "RemoteControlCompat"

        private var sRemoteControlClientClass: Class<*>? = null

        // RCC short for RemoteControlClient
        private var sRCCEditMetadataMethod: Method? = null
        private var sRCCSetPlayStateMethod: Method? = null
        private var sRCCSetTransportControlFlags: Method? = null

        private var sHasRemoteControlAPIs = false

        init {
            try {
                val classLoader = RemoteControlClientCompat::class.java.classLoader
                sRemoteControlClientClass = getActualRemoteControlClientClass(classLoader!!)
                // dynamically populate the playstate and flag values in case they change
                // in future versions.
                for (field in RemoteControlClientCompat::class.java.fields) {
                    try {
                        val realField = sRemoteControlClientClass!!.getField(field.name)
                        val realValue = realField.get(null)
                        field.set(null, realValue)
                    } catch (e: NoSuchFieldException) {
                        Log.w(TAG, "Could not get real field: " + field.name)
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "Error trying to pull field value for: " + field.name
                                + " " + e.message)
                    } catch (e: IllegalAccessException) {
                        Log.w(TAG, "Error trying to pull field value for: " + field.name + " " + e.message)
                    }

                }

                // get the required public methods on RemoteControlClient
                sRCCEditMetadataMethod = sRemoteControlClientClass!!.getMethod("editMetadata",
                        Boolean::class.javaPrimitiveType!!)
                sRCCSetPlayStateMethod = sRemoteControlClientClass!!.getMethod("setPlaybackState",
                        Int::class.javaPrimitiveType!!)
                sRCCSetTransportControlFlags = sRemoteControlClientClass!!.getMethod(
                        "setTransportControlFlags", Int::class.javaPrimitiveType!!)

                sHasRemoteControlAPIs = true
            } catch (e: ClassNotFoundException) {
                // Silently fail when running on an OS before ICS.
            } catch (e: NoSuchMethodException) {
            } catch (e: IllegalArgumentException) {
            } catch (e: SecurityException) {
            }

        }

        @Throws(ClassNotFoundException::class)
        fun getActualRemoteControlClientClass(classLoader: ClassLoader): Class<*> {
            return classLoader.loadClass("android.media.RemoteControlClient")
        }
    }
}
