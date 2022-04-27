package org.helllabs.modarchive.model

import android.text.Spanned
import androidx.core.text.toSpanned
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@SerialName("modarchive")
data class ModuleResult(
    @XmlElement(true) val sponsor: Sponsor = Sponsor(),
    @XmlElement(true) val error: String? = null,
    @XmlElement(true) val results: Int = 0,
    @XmlElement(true) val totalpages: Int = 0,
    @XmlElement(true) val module: Module = Module(),
) {
    fun hasSponsor(): Boolean {
        return sponsor.details.text.isNotEmpty()
    }
}

@Serializable
@SerialName("sponsor")
data class Sponsor(
    @XmlElement(true) val details: SponsorDetails = SponsorDetails()
)

@Serializable
@SerialName("details")
data class SponsorDetails(
    @XmlElement(true) val link: String = "",
    @XmlElement(true) val image: String = "",
    @XmlElement(true) val text: String = "",
    @XmlElement(true) val imagehtml: String = "",
)

@Serializable
@SerialName("module")
data class Module(
    @XmlElement(true) val filename: String = "",
    @XmlElement(true) val format: String = "",
    @XmlElement(true) val url: String = "",
    @XmlElement(true) val date: String = "",
    @XmlElement(true) val timestamp: Long = 0L,
    @XmlElement(true) val id: Int = 0,
    @XmlElement(true) val hash: String = "",
    @XmlElement(true) val featured: Featured = Featured(),
    @XmlElement(true) val favourites: Favourites = Favourites(),
    @XmlElement(true) val size: String = "",
    @XmlElement(true) val bytes: Int = 0,
    @XmlElement(true) val hits: Int = 0,
    @XmlElement(true) val infopage: String = "",
    @XmlElement(true) val songtitle: String = "",
    @XmlElement(true) val hidetext: Int = 0,
    @XmlElement(true) val comment: String = "",
    @XmlElement(true) val instruments: String = "",
    @XmlElement(true) val genreid: Int = 0,
    @XmlElement(true) val genretext: String = "",
    @XmlElement(true) val channels: Int = 0,
    @XmlElement(true) val overallRatings: OverallRatings = OverallRatings(),
    @XmlElement(true) val license: License = License(),
    @XmlElement(true) val artistInfo: ArtistInfo = ArtistInfo(),
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
        val title = if (songtitle.isNotEmpty()) songtitle.toSpanned() else "(untitled)"
        return title.toSpanned()
    }

    fun parseInstruments(): String {
        val lines = instruments.split("\n").toTypedArray()
        val buffer = StringBuilder()

        lines.map {
            val line = it.toSpanned()
            buffer.appendLine(line)
        }

        return buffer.toString()
    }

    fun parseComment(): String {
        val lines = comment.split("\n").toTypedArray()
        val buffer = StringBuilder()

        lines.map {
            val line = it.toSpanned()
            buffer.appendLine(line)
        }

        return buffer.toString()
    }
}

@Serializable
@SerialName("featured")
data class Featured(
    @XmlElement(true) val state: String = "",
    @XmlElement(true) val date: String = "",
    @XmlElement(true) val timestamp: String = "",
)

@Serializable
@SerialName("favourites")
data class Favourites(
    @XmlElement(true) val favoured: Int = 0,
    @XmlElement(true) val myfav: Int = 0,
)

@Serializable
@SerialName("overall_ratings")
data class OverallRatings(
    @XmlElement(true) val comment_rating: Double = 0.0,
    @XmlElement(true) val comment_total: Int = 0,
    @XmlElement(true) val review_rating: Int = 0,
    @XmlElement(true) val review_total: Int = 0,
)

@Serializable
@SerialName("license")
data class License(
    @XmlElement(true) val licenseid: String = "",
    @XmlElement(true) val title: String = "",
    @XmlElement(true) val description: String = "",
    @XmlElement(true) val imageurl: String = "",
    @XmlElement(true) val deedurl: String = "",
    @XmlElement(true) val legalurl: String = ""
)

@Serializable
@SerialName("artist_info")
data class ArtistInfo(
    @XmlElement(true) val artists: Int = 0,
    @XmlSerialName("artist", "", "") val artist: List<Artist> = emptyList(),
    @XmlElement(true) val guessed_artists: Int = 0,
    @XmlElement(true) val guessed_artist: GuessedArtists = GuessedArtists()
) {
    val guessedArtistList: List<String>
        get() = guessed_artist.alias
}

// https://modarchive.org/forums/index.php?topic=4713.0
// TODO: I'm not sure of this is correct, rare to see multiple guest artists.
@Serializable
@SerialName("guessed_artist")
data class GuessedArtists(
    @XmlSerialName("alias", "", "") val alias: List<String> = emptyList()
)

@Serializable
@SerialName("artist")
data class Artist(
    @XmlElement(true) val id: Int = 0,
    @XmlElement(true) val alias: String = "",
    @XmlElement(true) val profile: String = "",
    @XmlElement(true) val imageurl: String = "",
    @XmlElement(true) val imageurl_thumb: String = "",
    @XmlElement(true) val imageurl_icon: String = "",
    @XmlElement(true) val module_data: ModuleData = ModuleData()
)

@Serializable
@SerialName("module_data")
data class ModuleData(
    @XmlElement(true) val module_description: String = ""
)

@Serializable
@SerialName("modarchive")
data class SearchListResult(
    @XmlElement(true) val sponsor: Sponsor = Sponsor(),
    @XmlElement(true) val error: String? = null,
    @XmlElement(true) val results: Int = 0,
    @XmlElement(true) val totalpages: Int = 0,
    @XmlSerialName("module", "", "") val module: List<Module> = emptyList()
)

@Serializable
@SerialName("modarchive")
data class ArtistResult(
    @XmlElement(true) val sponsor: Sponsor = Sponsor(),
    @XmlElement(true) val error: String? = null,
    @XmlElement(true) val results: Int = 0,
    @XmlElement(true) val total_results: Int = 0,
    @XmlElement(true) val totalpages: Int = 0,
    @XmlElement(true) val items: Items = Items()
) {
    val listItems: List<Item>
        get() = items.item
}

@Serializable
@SerialName("items")
data class Items(
    @XmlSerialName("item", "", "") val item: List<Item> = emptyList()
)

@Serializable
@SerialName("item")
data class Item(
    @XmlElement(true) val id: Int = 0,
    @XmlElement(true) val alias: String = "",
    @XmlElement(true) val date: String = "",
    @XmlElement(true) val timestamp: Int = 0,
    @XmlElement(true) val lastseen: String = "",
    @XmlElement(true) val isartist: String = "",
    @XmlElement(true) val imageurl: String = "",
    @XmlElement(true) val imageurl_thumb: String = "",
    @XmlElement(true) val imageurl_icon: String = "",
    @XmlElement(true) val profile: String = ""
)
