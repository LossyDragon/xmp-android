package org.helllabs.android.xmp.model

import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.Path
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml
import org.helllabs.android.xmp.util.asHtml

/**
 * -- Main XML data files --
 * To interpret responses from mod archive.
 */

@Xml(name = "modarchive")
data class ModuleResult(
    @Element
    var sponsor: Sponsor? = null,

    @PropertyElement
    var error: String? = null,

    @PropertyElement
    var results: Int? = null,

    @PropertyElement
    var totalpages: Int? = null,

    @Element
    var module: Module? = null
) {
    override fun toString(): String {
        return "ModArchive(" +
            "sponsor=$sponsor, " + "results=$results, " +
            "totalpages=$totalpages, " + "module=$module" +
            ")"
    }

    fun hasSponsor(): Boolean {
        return sponsor?.details != null && !sponsor!!.details!!.text.isNullOrEmpty()
    }
}

@Xml(name = "modarchive")
data class ArtistResult(
    @Element
    var sponsor: Sponsor? = null,

    @PropertyElement
    var error: String? = null,

    @PropertyElement
    var results: Int? = null,

    @PropertyElement
    var total_results: Int? = null,

    @PropertyElement
    var totalpages: Int? = null,

    @Path("items")
    @Element
    var items: List<Item>? = null

)

@Xml(name = "modarchive")
data class SearchListResult(
    @Element
    var sponsor: Sponsor? = null,

    @PropertyElement
    var error: String? = null,

    @PropertyElement
    var results: Int? = null,

    @PropertyElement
    var totalpages: Int? = null,

    @Element
    var module: List<Module>? = null
) {
    override fun toString(): String {
        return "SearchListResult(" +
            "sponsor=$sponsor, " +
            "results=$results, " +
            "totalpages=$totalpages, " +
            "module=$module" +
            ")"
    }
}

/**
 * --  Various XML Element helpers --
 */
@Xml(name = "sponsor")
data class Sponsor(
    @Element
    var details: SponsorDetails? = null
)

@Xml(name = "details")
data class SponsorDetails(
    @PropertyElement
    var link: String? = null,

    @PropertyElement
    var image: String? = null,

    @PropertyElement
    var text: String? = null,

    @PropertyElement
    var imagehtml: String? = null
)

@Xml(name = "module")
data class Module(
    @PropertyElement
    var filename: String? = null,

    @PropertyElement
    var format: String? = null,

    @PropertyElement
    var url: String? = null,

    @PropertyElement
    var date: String? = null,

    @PropertyElement
    var timestamp: Long? = null,

    @PropertyElement
    var id: Int? = null,

    @PropertyElement
    var hash: String? = null,

    @Element
    var featured: Featured? = null,

    @Element
    var favourites: Favourites? = null,

    @PropertyElement
    var size: String? = null,

    @PropertyElement
    var bytes: Int? = null,

    @PropertyElement
    var hits: Int? = null,

    @PropertyElement
    var infopage: String? = null,

    @PropertyElement
    var songtitle: String? = null,

    @PropertyElement
    var hidetext: Int? = null,

    @PropertyElement
    var comment: String? = null,

    @PropertyElement
    var instruments: String? = null,

    @PropertyElement
    var genreid: Int? = null,

    @PropertyElement
    var genretext: String? = null,

    @PropertyElement
    var channels: Int? = null,

    @Element
    var overallRatings: OverallRatings? = null,

    @Element
    var license: License? = null,

    @Element
    var artistInfo: ArtistInfo? = null
) {

    fun getLicence(): License = license ?: License()

    fun getBytesFormatted(): Int = bytes?.div(1024) ?: 0

    fun getArtist(): String =
        artistInfo?.artist?.alias ?: artistInfo?.guessed_artist?.alias ?: "unknown"

    @JvmName("getFormatText")
    fun getFormat(): String = format.orEmpty()

    @JvmName("getFilenameText")
    fun getFilename(): String = filename.orEmpty()

    fun getSongTitle(): String =
        if (!songtitle.isNullOrEmpty()) songtitle.asHtml() else "(untitled)"

    fun parseComment(): String {
        val lines = comment?.split("\n")?.toTypedArray().orEmpty()
        val buffer = StringBuilder()

        lines.forEach {
            buffer.appendLine(it.asHtml())
        }

        return buffer.toString()
    }

    fun parseInstruments(): String {
        val lines = instruments?.split("\n")?.toTypedArray().orEmpty()
        val buffer = StringBuilder()

        lines.forEach {
            buffer.appendLine(it.asHtml())
        }

        return buffer.toString()
    }
}

@Xml(name = "featured")
data class Featured(
    @PropertyElement
    var state: String? = null,

    @PropertyElement
    var date: String? = null,

    @PropertyElement
    var timestamp: String? = null
)

@Xml(name = "favourites")
data class Favourites(
    @PropertyElement
    var favoured: Int? = null,

    @PropertyElement
    var myfav: Int? = null
)

@Xml(name = "overall_ratings")
data class OverallRatings(
    @PropertyElement
    var comment_rating: Double? = null,

    @PropertyElement
    var comment_total: Int? = null,

    @PropertyElement
    var review_rating: Int? = null,

    @PropertyElement
    var review_total: Int? = null
)

@Xml(name = "license")
data class License(
    @PropertyElement
    var licenseid: String? = null,

    @PropertyElement
    var title: String? = null,

    @PropertyElement
    var description: String? = null,

    @PropertyElement
    var imageurl: String? = null,

    @PropertyElement
    var deedurl: String? = null,

    @PropertyElement
    var legalurl: String? = null
) {
    fun getLegalUrl(): String = legalurl.orEmpty()

    fun getLegalTitle(): String = title.orEmpty()
}

@Xml(name = "artist_info")
data class ArtistInfo(
    @PropertyElement
    var artists: Int? = null,

    @Element
    var artist: Artist? = null,

    @PropertyElement
    var guessed_artists: Int? = null,

    @Element
    var guessed_artist: GuestArtist? = null
)

@Xml(name = "artist")
data class Artist(
    @PropertyElement
    var id: Int? = null,

    @PropertyElement
    var alias: String? = null,

    @PropertyElement
    var profile: String? = null,

    @PropertyElement
    var imageurl: String? = null,

    @PropertyElement
    var imageurl_thumb: String? = null,

    @PropertyElement
    var imageurl_icon: String? = null,

    @Element
    var module_data: ModuleData? = null
)

@Xml(name = "module_data")
data class ModuleData(
    @PropertyElement
    var module_description: String? = null
)

@Xml(name = "guessed_artist")
data class GuestArtist(
    @PropertyElement
    var alias: String? = null
)

@Xml(name = "item")
data class Item(
    @PropertyElement
    var id: Int? = null,

    @PropertyElement
    var alias: String? = null,

    @PropertyElement
    var date: String? = null,

    @PropertyElement
    var timestamp: Int? = null,

    @PropertyElement
    var lastseen: String? = null,

    @PropertyElement
    var isartist: String? = null,

    @PropertyElement
    var imageurl: String? = null,

    @PropertyElement
    var imageurl_thumb: String? = null,

    @PropertyElement
    var imageurl_icon: String? = null,

    @PropertyElement
    var profile: String? = null
)
