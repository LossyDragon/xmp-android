package org.helllabs.android.xmp.serializers

import android.net.Uri
import androidx.core.net.toUri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object UriSerializer : KSerializer<Uri> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "Uri",
        kind = PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: Uri) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Uri {
        val string = decoder.decodeString()
        return if (string.isEmpty()) Uri.EMPTY else string.toUri()
    }
}
