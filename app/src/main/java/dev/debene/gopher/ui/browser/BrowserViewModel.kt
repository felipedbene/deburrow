package dev.debene.gopher.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.debene.gopher.data.DownloadStore
import dev.debene.gopher.data.GopherRepository
import dev.debene.gopher.data.LibraryRepository
import dev.debene.gopher.di.AppContainer
import dev.debene.gopher.protocol.GopherItem
import dev.debene.gopher.protocol.GopherParser
import dev.debene.gopher.protocol.GopherRequest
import dev.debene.gopher.protocol.GopherType
import dev.debene.gopher.protocol.GopherUrl

/**
 * Drives the single browser screen. Replaces the J2ME `PocketGopher` MIDlet's tangle of
 * `commandAction` branches, threads, and the `history`/`previous_dir` stack juggling with
 * unidirectional state ([uiState]) plus one-shot [events].
 */
class BrowserViewModel(
    private val gopher: GopherRepository,
    private val library: LibraryRepository,
    private val downloads: DownloadStore,
    private val loadHome: () -> String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val _events = Channel<BrowserEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Pages visited before the current one. `null` represents the home page. */
    private val backStack = ArrayDeque<GopherRequest?>()
    private var current: GopherRequest? = null

    /** URL of the current page, observed to keep the bookmark star in sync. */
    private val currentUrl = MutableStateFlow<String?>(null)

    private var loadJob: Job? = null

    init {
        observeBookmarkState()
        goHome()
    }

    /** Keeps [BrowserUiState.isBookmarked] in sync with the current page. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeBookmarkState() {
        viewModelScope.launch {
            currentUrl
                .flatMapLatest { url -> if (url == null) flowOf(false) else library.isBookmarked(url) }
                .collect { bookmarked -> _uiState.update { it.copy(isBookmarked = bookmarked) } }
        }
    }

    // --- Navigation entry points ---

    fun goHome() = loadPage(null, pushCurrent = true)

    fun reload() = loadPage(current, pushCurrent = false, forceReload = true)

    fun stop() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = false) }
    }

    fun goBack() {
        if (backStack.isEmpty()) return
        val previous = backStack.removeLast()
        loadPage(previous, pushCurrent = false)
    }

    fun navigateToAddress(input: String) {
        val request = GopherUrl.parse(input)
        if (request == null) {
            emit(BrowserEvent.Message("Invalid Gopher URL"))
            return
        }
        loadPage(request, pushCurrent = true)
    }

    /** Handles a tap on a menu item, dispatching by type (ports `loadItem`). */
    fun open(item: GopherItem) {
        if (!item.isSelectable) return
        val tls = current?.tls == true
        when (item.type.kind) {
            GopherType.Kind.MENU,
            GopherType.Kind.TEXT,
            GopherType.Kind.IMAGE -> loadPage(item.toRequest(tls), pushCurrent = true)

            GopherType.Kind.SEARCH -> emit(BrowserEvent.ShowSearchDialog(item))
            GopherType.Kind.HTML -> emit(BrowserEvent.OpenWebUrl(item.selector))
            GopherType.Kind.BINARY -> download(item)
            GopherType.Kind.TELNET -> emit(BrowserEvent.Message("Telnet sessions aren't supported"))
            GopherType.Kind.INFO, GopherType.Kind.ERROR -> Unit
        }
    }

    /** Completes a type-7 search: append the query to the selector and fetch as a menu. */
    fun submitSearch(item: GopherItem, query: String) {
        val tls = current?.tls == true
        val request = item.toRequest(tls).let {
            it.copy(selector = it.selector + "\t" + query)
        }
        viewModelScope.launch { library.recordSearch(query, request) }
        loadPage(request, pushCurrent = true)
    }

    // --- Library actions ---

    fun toggleBookmark() {
        val req = current ?: return
        val url = req.url
        viewModelScope.launch {
            if (_uiState.value.isBookmarked) library.removeBookmark(url)
            else library.addBookmark(req, _uiState.value.title)
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    // --- Core loader ---

    private fun loadPage(request: GopherRequest?, pushCurrent: Boolean, forceReload: Boolean = false) {
        // A type-7 target with no query yet (e.g. a Veronica bookmark or a typed search URL)
        // should prompt for the query rather than fetch an empty search.
        if (request != null && request.type.kind == GopherType.Kind.SEARCH &&
            !request.selector.contains('\t')
        ) {
            emit(
                BrowserEvent.ShowSearchDialog(
                    GopherItem(
                        type = GopherType.SEARCH,
                        display = request.title,
                        selector = request.selector,
                        host = request.host,
                        port = request.port,
                    )
                )
            )
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val rendered = render(request, forceReload)
                if (pushCurrent) backStack.addLast(current)
                current = request
                currentUrl.value = request?.url
                _uiState.update {
                    it.copy(
                        title = rendered.title,
                        addressText = request?.url ?: "",
                        content = rendered.content,
                        isLoading = false,
                        canGoBack = backStack.isNotEmpty(),
                        current = request,
                    )
                }
                if (request != null && rendered.recordable) {
                    library.recordVisit(request, rendered.title)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Can't fetch the requested item")
                }
            }
        }
    }

    private suspend fun render(request: GopherRequest?, forceReload: Boolean): Rendered {
        if (request == null) {
            return Rendered(PageContent.Menu(GopherParser.parseMenu(loadHome())), "Gopher", recordable = false)
        }
        val bytes = gopher.fetch(request, forceReload)
        return when (request.type.kind) {
            // A type-7 search returns a Gopher menu of results — parse it like a directory.
            GopherType.Kind.MENU, GopherType.Kind.SEARCH ->
                Rendered(PageContent.Menu(GopherParser.parseMenu(bytes.toString(Charsets.UTF_8))), request.title)
            GopherType.Kind.IMAGE ->
                Rendered(PageContent.Image(bytes), request.title, recordable = false)
            else -> // TEXT and anything else viewable as text
                Rendered(PageContent.Text(normaliseText(bytes.toString(Charsets.UTF_8))), request.title)
        }
    }

    private fun download(item: GopherItem) {
        val tls = current?.tls == true
        viewModelScope.launch {
            emit(BrowserEvent.Message("Downloading ${item.display.trim()}…"))
            try {
                val bytes = gopher.fetch(item.toRequest(tls), forceReload = true)
                val uri = downloads.save(item, bytes)
                emit(BrowserEvent.Message("Saved to Downloads ($uri)"))
            } catch (e: Exception) {
                emit(BrowserEvent.Message("Download failed: ${e.message}"))
            }
        }
    }

    private fun emit(event: BrowserEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    /** Normalises Gopher text bodies: CR/CRLF -> LF, drop a trailing lone "." line. */
    private fun normaliseText(raw: String): String {
        val unified = raw.replace("\r\n", "\n").replace('\r', '\n')
        return unified.removeSuffix("\n.\n").removeSuffix("\n.")
    }

    private data class Rendered(
        val content: PageContent,
        val title: String,
        val recordable: Boolean = true,
    )

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                    BrowserViewModel(
                        gopher = container.gopherRepository,
                        library = container.libraryRepository,
                        downloads = container.downloadStore,
                        loadHome = container::loadHomeMenu,
                    ) as T
            }
    }
}
