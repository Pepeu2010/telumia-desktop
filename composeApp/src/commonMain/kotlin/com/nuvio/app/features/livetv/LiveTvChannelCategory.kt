package com.nuvio.app.features.livetv

/** Classify only explicit catalog genres and playlist groups, never channel names. */
internal enum class LiveTvChannelCategory {
    NEWS, ENTERTAINMENT, SPORTS, KIDS, MOVIES, MUSIC, DOCUMENTARY, OTHER, UNCATEGORIZED;

    companion object {
        fun fromGroup(group: String?): LiveTvChannelCategory = when (group?.trim()?.lowercase().orEmpty()) {
            "jornalismo", "notícias", "noticias", "news", "information", "informação", "informacao" -> NEWS
            "entretenimento", "entertainment", "variedades", "variety" -> ENTERTAINMENT
            "esportes", "esporte", "desporto", "sports", "sport" -> SPORTS
            "infantil", "kids", "children" -> KIDS
            "filmes", "movies", "cinema" -> MOVIES
            "música", "musica", "music" -> MUSIC
            "documentários", "documentarios", "documentary", "documentaries" -> DOCUMENTARY
            "" -> UNCATEGORIZED
            else -> OTHER
        }

        fun fromGenres(genres: List<String>): LiveTvChannelCategory {
            if (genres.isEmpty()) return UNCATEGORIZED
            val explicit = genres.map(::fromGroup).filter { it != OTHER && it != UNCATEGORIZED }.distinct()
            return when (explicit.size) {
                1 -> explicit.single()
                0 -> if (genres.all { fromGroup(it) == OTHER }) OTHER else UNCATEGORIZED
                else -> UNCATEGORIZED
            }
        }
    }
}
