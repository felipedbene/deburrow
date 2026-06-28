package dev.debene.gopher.di

import android.content.Context
import dev.debene.gopher.data.DownloadStore
import dev.debene.gopher.data.GopherRepository
import dev.debene.gopher.data.LibraryRepository
import dev.debene.gopher.data.db.AppDatabase
import dev.debene.gopher.protocol.GopherClient

/**
 * Lightweight manual DI graph (preferred over Hilt for an app this size). Holds the
 * singletons the ViewModels depend on. Built once in [dev.debene.gopher.GopherApp].
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    private val gopherClient: GopherClient by lazy { GopherClient() }

    val gopherRepository: GopherRepository by lazy {
        GopherRepository(gopherClient, database.cacheDao())
    }

    val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(
            bookmarkDao = database.bookmarkDao(),
            historyDao = database.historyDao(),
            searchHistoryDao = database.searchHistoryDao(),
        )
    }

    val downloadStore: DownloadStore by lazy { DownloadStore(appContext) }

    /** Loads the bundled start page (`assets/home.txt`), mirroring the old `/home.txt` resource. */
    fun loadHomeMenu(): String =
        appContext.assets.open("home.txt").use { it.readBytes().toString(Charsets.UTF_8) }
}
