package org.helllabs.android.xmp.core

import android.net.Uri
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import androidx.core.net.toUri

/**
 * Moshi adapter to handle URI parsing
 */
@Suppress("unused")
class UriAdapter {
    @ToJson
    fun toJson(uri: Uri): String = uri.toString()

    @FromJson
    fun fromJson(uriString: String): Uri = uriString.toUri()
}
