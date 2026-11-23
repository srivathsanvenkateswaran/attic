package app.attic.player.playback

/** A hint for the player that matches the file. Null leaves it to detect the format itself. */
fun mimeTypeFor(key: String): String? =
    when (key.substringAfterLast('.').lowercase()) {
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "flac" -> "audio/flac"
        "ogg", "opus" -> "audio/ogg"
        "wav" -> "audio/wav"
        else -> null
    }
