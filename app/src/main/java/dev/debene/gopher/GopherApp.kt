package dev.debene.gopher

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import dev.debene.gopher.di.AppContainer
import dev.debene.gopher.protocol.GopherRequest
import dev.debene.gopher.protocol.GopherType

class GopherApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        seedDefaultBookmarks()
    }

    /** Seeds the debene.dev gopherholes as bookmarks the first time the app runs. The prefs
     *  flag means deleting them afterwards is respected (they won't be re-added). */
    private fun seedDefaultBookmarks() {
        val prefs = getSharedPreferences("gopher_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED, false)) return
        appScope.launch {
            val defaults = listOf(
                GopherRequest("gopher.debene.dev", 70, GopherType.DIRECTORY) to "gopher-cta — live CTA 'L' trains",
                GopherRequest("gopher.debene.dev", 7071, GopherType.DIRECTORY) to "Phlog — the debene.dev blog",
                GopherRequest("gopher.debene.dev", 7072, GopherType.DIRECTORY) to "Ask the Deck — live tarot",
                GopherRequest("gopher.floodgap.com", 70, GopherType.DIRECTORY) to "Floodgap Systems",
                GopherRequest("gopher.floodgap.com", 70, GopherType.SEARCH, "/v2/vs") to "Veronica-2 — search Gopherspace",
                GopherRequest("sdf.org", 70, GopherType.DIRECTORY) to "SDF Public Access UNIX",
            )
            defaults.forEach { (req, title) -> container.libraryRepository.addBookmark(req, title) }
            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
        }
    }

    companion object {
        private const val KEY_SEEDED = "default_bookmarks_seeded_v1"
    }
}
