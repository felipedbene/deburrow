package dev.debene.gopher.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dev.debene.gopher.data.LibraryRepository
import dev.debene.gopher.data.db.BookmarkEntity
import dev.debene.gopher.data.db.HistoryEntity
import dev.debene.gopher.data.db.SearchHistoryEntity
import dev.debene.gopher.di.AppContainer

/** Exposes the persisted library lists (bookmarks, history, searches) to their screens. */
class LibraryViewModel(private val library: LibraryRepository) : ViewModel() {

    val bookmarks: StateFlow<List<BookmarkEntity>> =
        library.bookmarks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> =
        library.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searches: StateFlow<List<SearchHistoryEntity>> =
        library.searches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeBookmark(url: String) = viewModelScope.launch { library.removeBookmark(url) }
    fun clearHistory() = viewModelScope.launch { library.clearHistory() }
    fun clearSearches() = viewModelScope.launch { library.clearSearches() }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                    LibraryViewModel(container.libraryRepository) as T
            }
    }
}
