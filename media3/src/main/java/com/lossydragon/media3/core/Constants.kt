package com.lossydragon.media3.core

object Constants {
    const val ROOM_DATABASE_NAME: String = "xmp_database"
    const val HTTP_BASE_URL: String = "https://api.modarchive.org"
    val UNSUPPORTED_EXTENSIONS = setOf("ahx", "hvl", "mo3")
    val SKIP_EXTENSIONS = setOf(
        "txt", "pdf", "doc", "docx", "rtf", "nfo", "diz", "me",
        "jpg", "jpeg", "png", "gif", "bmp", "webp",
        "zip", "rar", "7z", "gz", "tar",
        "mp3", "flac", "ogg", "wav", "mp4", "avi",
        "xml", "json", "html", "htm", "css", "js",
    )
}
