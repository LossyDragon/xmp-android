package org.helllabs.android.xmp.modarchive.request

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.UnsupportedEncodingException

import org.helllabs.android.xmp.modarchive.model.Artist
import org.helllabs.android.xmp.modarchive.model.Sponsor
import org.helllabs.android.xmp.modarchive.response.ArtistResponse
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse
import org.helllabs.android.xmp.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

class ArtistRequest
@Throws(UnsupportedEncodingException::class)
constructor(key: String, request: String, parameter: String) : ModArchiveRequest(key, request, parameter) {

    override fun xmlParse(result: String): ModArchiveResponse {
        val artistList = ArtistResponse()
        var artist: Artist? = null
        var sponsor: Sponsor? = null

        try {
            val xmlFactoryObject = XmlPullParserFactory.newInstance()
            val myparser = xmlFactoryObject.newPullParser()
            val stream = ByteArrayInputStream(result.toByteArray())
            myparser.setInput(stream, null)

            var event = myparser.eventType
            var text = ""
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val start = myparser.name
                        if (start == "item") {
                            artist = Artist() // NOPMD
                        } else if (start == "sponsor") {
                            sponsor = Sponsor()    // NOPMD
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
                                    sponsor = null    // NOPMD
                                }
                            }
                        } else {
                            when (end) {
                                "error" -> return SoftErrorResponse(text)
                                "id" -> artist!!.id = java.lang.Long.parseLong(text)
                                "alias" -> artist!!.alias = text
                                "item" -> artistList.add(artist!!)
                            }
                        }
                    }
                    else -> {
                    }
                }
                event = myparser.next()
            }
        } catch (e: XmlPullParserException) {
            Log.e(TAG, "XmlPullParserException: " + e.message)
            return HardErrorResponse(e)
        } catch (e: IOException) {
            Log.e(TAG, "IOException: " + e.message)
            return HardErrorResponse(e)
        }

        return artistList
    }

    companion object {
        private const val TAG = "ArtistListRequest"
    }
}
