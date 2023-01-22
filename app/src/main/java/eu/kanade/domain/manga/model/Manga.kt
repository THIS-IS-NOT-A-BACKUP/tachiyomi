package eu.kanade.domain.manga.model

import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.reader.setting.OrientationType
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.TriStateFilter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// TODO: move these into the domain model
val Manga.readingModeType: Long
    get() = viewerFlags and ReadingModeType.MASK.toLong()

val Manga.orientationType: Long
    get() = viewerFlags and OrientationType.MASK.toLong()

val Manga.downloadedFilter: TriStateFilter
    get() {
        if (forceDownloaded()) return TriStateFilter.ENABLED_IS
        return when (downloadedFilterRaw) {
            Manga.CHAPTER_SHOW_DOWNLOADED -> TriStateFilter.ENABLED_IS
            Manga.CHAPTER_SHOW_NOT_DOWNLOADED -> TriStateFilter.ENABLED_NOT
            else -> TriStateFilter.DISABLED
        }
    }
fun Manga.chaptersFiltered(): Boolean {
    return unreadFilter != TriStateFilter.DISABLED ||
        downloadedFilter != TriStateFilter.DISABLED ||
        bookmarkedFilter != TriStateFilter.DISABLED
}
fun Manga.forceDownloaded(): Boolean {
    return favorite && Injekt.get<BasePreferences>().downloadedOnly().get()
}

fun Manga.toSManga(): SManga = SManga.create().also {
    it.url = url
    it.title = title
    it.artist = artist
    it.author = author
    it.description = description
    it.genre = genre.orEmpty().joinToString()
    it.status = status.toInt()
    it.thumbnail_url = thumbnailUrl
    it.initialized = initialized
}

fun Manga.copyFrom(other: SManga): Manga {
    val author = other.author ?: author
    val artist = other.artist ?: artist
    val description = other.description ?: description
    val genres = if (other.genre != null) {
        other.getGenres()
    } else {
        genre
    }
    val thumbnailUrl = other.thumbnail_url ?: thumbnailUrl
    return this.copy(
        author = author,
        artist = artist,
        description = description,
        genre = genres,
        thumbnailUrl = thumbnailUrl,
        status = other.status.toLong(),
        updateStrategy = other.update_strategy,
        initialized = other.initialized && initialized,
    )
}

fun SManga.toDomainManga(sourceId: Long): Manga {
    return Manga.create().copy(
        url = url,
        title = title,
        artist = artist,
        author = author,
        description = description,
        genre = getGenres(),
        status = status.toLong(),
        thumbnailUrl = thumbnail_url,
        updateStrategy = update_strategy,
        initialized = initialized,
        source = sourceId,
    )
}

fun Manga.isLocal(): Boolean = source == LocalSource.ID

fun Manga.hasCustomCover(coverCache: CoverCache = Injekt.get()): Boolean {
    return coverCache.getCustomCoverFile(id).exists()
}
