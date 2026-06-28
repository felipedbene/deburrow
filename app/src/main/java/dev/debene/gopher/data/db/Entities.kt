package dev.debene.gopher.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A saved location. [url] is the canonical gopher URL and acts as the natural key. */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val url: String,
    val title: String,
    val createdAt: Long,
)

/** An automatically recorded visit. */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val visitedAt: Long,
)

/** A past type-7 search query, for autocomplete / reuse. */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val host: String,
    val selector: String,
    val searchedAt: Long,
)

/** Cached response body keyed by canonical URL, with a fetch timestamp for TTL checks. */
@Entity(tableName = "cache")
data class CacheEntity(
    @PrimaryKey val url: String,
    val body: ByteArray,
    val fetchedAt: Long,
) {
    // ByteArray needs explicit equals/hashCode for data-class semantics.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CacheEntity) return false
        return url == other.url && fetchedAt == other.fetchedAt && body.contentEquals(other.body)
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + fetchedAt.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }
}
