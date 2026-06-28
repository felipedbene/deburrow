package dev.debene.gopher.ui.browser

import dev.debene.gopher.protocol.GopherItem
import dev.debene.gopher.protocol.GopherRequest

/** What the browser is currently rendering. */
sealed interface PageContent {
    data object Blank : PageContent
    data class Menu(val items: List<GopherItem>) : PageContent
    data class Text(val text: String) : PageContent
    data class Image(val bytes: ByteArray) : PageContent {
        override fun equals(other: Any?) =
            other is Image && bytes.contentEquals(other.bytes)
        override fun hashCode() = bytes.contentHashCode()
    }
}

data class BrowserUiState(
    val title: String = "Gopher",
    val addressText: String = "",
    val content: PageContent = PageContent.Blank,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBookmarked: Boolean = false,
    val canGoBack: Boolean = false,
    val current: GopherRequest? = null,
)

/** One-shot effects the UI reacts to (navigation away, dialogs, transient messages). */
sealed interface BrowserEvent {
    data class OpenWebUrl(val url: String) : BrowserEvent
    data class ShowSearchDialog(val item: GopherItem) : BrowserEvent
    data class Message(val text: String) : BrowserEvent
}
