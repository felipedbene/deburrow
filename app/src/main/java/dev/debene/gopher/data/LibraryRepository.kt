package dev.debene.gopher.data

import kotlinx.coroutines.flow.Flow
import dev.debene.gopher.data.db.BookmarkDao
import dev.debene.gopher.data.db.BookmarkEntity
import dev.debene.gopher.data.db.HistoryDao
import dev.debene.gopher.data.db.HistoryEntity
import dev.debene.gopher.data.db.SearchHistoryDao
import dev.debene.gopher.data.db.SearchHistoryEntity
import dev.debene.gopher.protocol.GopherRequest

/**
 * Persistence for the user's library: bookmarks, browse history, and past search queries.
 * Wraps the Room DAOs behind a small domain-friendly surface used by the ViewModels.
 */
class LibraryRepository(
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    // --- Bookmarks ---
    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.observeAll()

    fun isBookmarked(url: String): Flow<Boolean> = bookmarkDao.isBookmarked(url)

    suspend fun addBookmark(request: GopherRequest, title: String) =
        bookmarkDao.upsert(BookmarkEntity(request.url, title.ifBlank { request.title }, now()))

    suspend fun removeBookmark(url: String) = bookmarkDao.delete(url)

    // --- Browse history ---
    val history: Flow<List<HistoryEntity>> = historyDao.observeRecent()

    suspend fun recordVisit(request: GopherRequest, title: String) =
        historyDao.insert(HistoryEntity(url = request.url, title = title, visitedAt = now()))

    suspend fun clearHistory() = historyDao.clear()

    // --- Search history ---
    val searches: Flow<List<SearchHistoryEntity>> = searchHistoryDao.observeRecent()

    suspend fun recordSearch(query: String, request: GopherRequest) =
        searchHistoryDao.insert(
            SearchHistoryEntity(
                query = query,
                host = request.host,
                selector = request.selector,
                searchedAt = now(),
            )
        )

    suspend fun clearSearches() = searchHistoryDao.clear()
}
