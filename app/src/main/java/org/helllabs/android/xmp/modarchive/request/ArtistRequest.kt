package org.helllabs.android.xmp.modarchive.request

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import org.helllabs.android.xmp.modarchive.model.Artist
import org.helllabs.android.xmp.modarchive.model.Sponsor
import org.helllabs.android.xmp.modarchive.response.ArtistResponse
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse
import org.helllabs.android.xmp.util.logE
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

class ArtistRequest : ModArchiveRequest {

    constructor(key: String, request: String) : super(key, request)
    constructor(key: String, request: String, parameter: String?) : super(key, request, parameter)
    constructor(key: String, request: String, parameter: Long) : this(
        key,
        request,
        parameter.toString()
    )

    override fun xmlParse(result: String): ModArchiveResponse {
        val artistList = ArtistResponse()
        var artist: Artist? = null
        var sponsor: Sponsor? = null
        try {
            val xmlFactoryObject = XmlPullParserFactory.newInstance()
            val myparser = xmlFactoryObject.newPullParser()
            val stream: InputStream = ByteArrayInputStream(result.toByteArray())
            myparser.setInput(stream, null)
            var event = myparser.eventType
            var text = ""
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val start = myparser.name
                        if (start == "item") {
                            artist = Artist()
                        } else if (start == "sponsor") {
                            sponsor = Sponsor()
                        }
                    }
                    XmlPullParser.TEXT -> text = myparser.text.trim { it <= ' ' }
                    XmlPullParser.END_TAG -> {
                        val end = myparser.name
                        if (sponsor != null) {
                            when (end) {
                                "text" -> sponsor.name = text
                                "link" -> sponsor.link = text
                                "sponsor" -> {
                                    artistList.sponsor = sponsor
                                    sponsor = null
                                }
                            }
                        } else {
                            when (end) {
                                "error" -> return SoftErrorResponse(text)
                                "id" -> artist!!.id = text.toLong()
                                "alias" -> artist!!.setAlias(text)
                                "item" -> artistList.add(artist)
                            }
                        }
                    }
                    else -> {
                    }
                }
                event = myparser.next()
            }
        } catch (e: XmlPullParserException) {
            logE("XmlPullParserException: " + e.message)
            return HardErrorResponse(e)
        } catch (e: IOException) {
            logE("IOException: " + e.message)
            return HardErrorResponse(e)
        }
        return artistList
    }

    companion object {
        private const val TAG = "ArtistListRequest"
    }
}
