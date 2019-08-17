package org.helllabs.android.xmp.modarchive.request

import org.helllabs.android.xmp.modarchive.model.Artist
import org.helllabs.android.xmp.modarchive.model.Module
import org.helllabs.android.xmp.modarchive.model.Sponsor
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse
import org.helllabs.android.xmp.modarchive.response.ModuleResponse
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse
import org.helllabs.android.xmp.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.UnsupportedEncodingException

class ModuleRequest : ModArchiveRequest {

    constructor(key: String, request: String) : super(key, request)

    @Throws(UnsupportedEncodingException::class)
    constructor(key: String, request: String, parameter: String) : super(key, request, parameter)

    @Throws(UnsupportedEncodingException::class)
    constructor(key: String, request: String, parameter: Long) : this(key, request, parameter.toString())

    override fun xmlParse(result: String): ModArchiveResponse {
        val moduleList = ModuleResponse()
        var module: Module? = null
        var sponsor: Sponsor? = null
        var inArtistInfo = false
        val unsupported = listOf(*UNSUPPORTED)

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
                        when (myparser.name) {
                            "module" -> module = Module()
                            "artist_info" -> inArtistInfo = true
                            "sponsor" -> sponsor = Sponsor()
                        }
                    }
                    XmlPullParser.TEXT -> text = myparser.text.trim { it <= ' ' }
                    XmlPullParser.END_TAG -> {
                        val end = myparser.name
                        //Log.d(TAG, "name=" + name + " text=" + text);
                        when {
                            sponsor != null -> when (end) {
                                "text" -> sponsor.name = text
                                "link" -> sponsor.link = text
                                "sponsor" -> {
                                    moduleList.sponsor = sponsor
                                    sponsor = null
                                }
                            }
                            end == "error" -> return SoftErrorResponse(text)
                            end == "artist_info" -> inArtistInfo = false
                            module != null -> when (end) {
                                "filename" -> module.filename = text
                                "format" -> module.format = text
                                "url" -> module.url = text
                                "bytes" -> module.bytes = Integer.parseInt(text)
                                "songtitle" -> module.songTitle = text
                                "alias" ->
                                    // Use non-guessed artist if available
                                    if (module.artist == Artist.UNKNOWN) {
                                        module.artist = text
                                    }
                                "title" -> module.license = text
                                "description" -> module.licenseDescription = text
                                "legalurl" -> module.legalUrl = text
                                "instruments" -> module.instruments = text
                                "id" -> if (!inArtistInfo) {
                                    module.id = java.lang.Long.parseLong(text)
                                }
                                "module" -> if (!unsupported.contains(module.format)) {
                                    moduleList.add(module)
                                }
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

        return moduleList
    }

    companion object {
        private const val TAG = "ModuleRequest"
        private val UNSUPPORTED = arrayOf("AHX", "HVL", "MO3")
    }
}
