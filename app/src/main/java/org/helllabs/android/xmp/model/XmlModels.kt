@file:Suppress("PropertyName")

package org.helllabs.android.xmp.model

import android.os.Build
import android.text.Html
import android.text.Spanned
import androidx.core.text.toSpanned
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.adaptivity.xmlutil.serialization.*

@Serializable
@SerialName("modarchive")
data class ModuleResult(
    @XmlElement val sponsor: Sponsor = Sponsor(),
    @XmlElement val error: String? = null,
    @XmlElement val results: Int = 0,
    @XmlElement val totalpages: Int = 0,
    @XmlElement val module: Module = Module()
) {
    @Transient
    val hasSponsor: Boolean = sponsor.details.text.isNotEmpty()
}

@Serializable
@SerialName("sponsor")
data class Sponsor(@XmlElement val details: SponsorDetails = SponsorDetails())

@Serializable
@SerialName("details")
data class SponsorDetails(
    @XmlElement val link: String = "",
    @XmlElement val image: String = "",
    @XmlElement val text: String = "",
    @XmlElement val imagehtml: String = ""
)

@Serializable
@SerialName("module")
data class Module(
    @XmlElement val filename: String = "",
    @XmlElement val format: String = "",
    @XmlElement val url: String = "",
    @XmlElement val date: String = "",
    @XmlElement val timestamp: Long = 0L,
    @XmlElement val id: Int = 0,
    @XmlElement val hash: String = "",
    @XmlElement val featured: Featured = Featured(),
    @XmlElement val favourites: Favourites = Favourites(),
    @XmlElement val size: String = "",
    @XmlElement val bytes: Int = 0,
    @XmlElement val hits: Int = 0,
    @XmlElement val infopage: String = "",
    @XmlElement val songtitle: String = "",
    @XmlElement val hidetext: Int = 0,
    @XmlElement val comment: String = "",
    @XmlElement val instruments: String = "",
    @XmlElement val genreid: Int = 0,
    @XmlElement val genretext: String = "",
    @XmlElement val channels: Int = 0,
    @XmlElement val overallRatings: OverallRatings = OverallRatings(),
    @XmlElement val license: License = License(),
    @XmlElement val artistInfo: ArtistInfo = ArtistInfo()
) {
    val byteSize: Int
        get() = bytes.div(1024)

    fun getArtist(): String {
        with(artistInfo) {
            artist.firstOrNull {
                return it.alias
            }

            guessedArtistList.firstOrNull {
                return it
            }

            return "unknown"
        }
    }

    fun getSongTitle(): Spanned {
        val title = if (songtitle.isNotEmpty()) songtitle.asHtml() else "(untitled)"
        return title.toSpanned()
    }

    fun parseInstruments(): String {
        val lines = instruments.split("\n").toTypedArray()
        val buffer = StringBuilder()

        lines.map {
            val line = it.asHtml()
            buffer.appendLine(line)
        }

        return buffer.toString()
    }

    fun parseComment(): String {
        val lines = comment.split("\n").toTypedArray()
        val buffer = StringBuilder()

        lines.map {
            val line = it.asHtml()
            buffer.appendLine(line)
        }

        return buffer.toString()
    }
}

@Serializable
@SerialName("featured")
data class Featured(
    @XmlElement val state: String = "",
    @XmlElement val date: String = "",
    @XmlElement val timestamp: String = ""
)

@Serializable
@SerialName("favourites")
data class Favourites(@XmlElement val favoured: Int = 0, @XmlElement val myfav: Int = 0)

@Serializable
@SerialName("overall_ratings")
data class OverallRatings(
    @XmlElement val comment_rating: Double = 0.0,
    @XmlElement val comment_total: Int = 0,
    @XmlElement val review_rating: Int = 0,
    @XmlElement val review_total: Int = 0
)

@Serializable
@SerialName("license")
data class License(
    @XmlElement val licenseid: String = "",
    @XmlElement val title: String = "",
    @XmlElement val description: String = "",
    @XmlElement val imageurl: String = "",
    @XmlElement val deedurl: String = "",
    @XmlElement val legalurl: String = ""
)

@Serializable
@SerialName("artist_info")
data class ArtistInfo(
    @XmlElement val artists: Int = 0,
    @XmlSerialName("artist", "", "") val artist: List<Artist> = emptyList(),
    @XmlElement val guessed_artists: Int = 0,
    @XmlElement val guessed_artist: GuessedArtists = GuessedArtists()
) {
    val guessedArtistList: List<String>
        get() = guessed_artist.alias
}

// (Link Dead) https://modarchive.org/forums/index.php?topic=4713.0
// Not even TMA knows much about this.
// NOTE: I'm not sure of this is correct, rare to see multiple guest artists.
@Serializable
@SerialName("guessed_artist")
data class GuessedArtists(@XmlSerialName("alias", "", "") val alias: List<String> = emptyList())

@Serializable
@SerialName("artist")
data class Artist(
    @XmlElement val id: Int = 0,
    @XmlElement val alias: String = "",
    @XmlElement val profile: String = "",
    @XmlElement val imageurl: String = "",
    @XmlElement val imageurl_thumb: String = "",
    @XmlElement val imageurl_icon: String = "",
    @XmlElement val module_data: ModuleData = ModuleData()
)

@Serializable
@SerialName("module_data")
data class ModuleData(@XmlElement val module_description: String = "")

@Serializable
@SerialName("modarchive")
data class SearchListResult(
    @XmlElement val sponsor: Sponsor = Sponsor(),
    @XmlElement val error: String? = null,
    @XmlElement val results: Int = 0,
    @XmlElement val totalpages: Int = 0,
    @XmlSerialName("module", "", "") val module: List<Module> = emptyList()
)

@Serializable
@SerialName("modarchive")
data class ArtistResult(
    @XmlElement val sponsor: Sponsor = Sponsor(),
    @XmlElement val error: String? = null,
    @XmlElement val results: Int = 0,
    @XmlElement val total_results: Int = 0,
    @XmlElement val totalpages: Int = 0,
    @XmlElement val items: Items = Items()
) {
    val listItems: List<Item>
        get() = items.item
}

@Serializable
@SerialName("items")
data class Items(@XmlSerialName("item", "", "") val item: List<Item> = emptyList())

@Serializable
@SerialName("item")
data class Item(
    @XmlElement val id: Int = 0,
    @XmlElement val alias: String = "",
    @XmlElement val date: String = "",
    @XmlElement val timestamp: Int = 0,
    @XmlElement val lastseen: String = "",
    @XmlElement val isartist: String = "",
    @XmlElement val imageurl: String = "",
    @XmlElement val imageurl_thumb: String = "",
    @XmlElement val imageurl_icon: String = "",
    @XmlElement val profile: String = ""
)

/**
 * General helper functions related to Strings
 */
private fun String?.asHtml(): Spanned {
    if (this.isNullOrEmpty()) {
        return "".toSpanned()
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(this)
    }
}
