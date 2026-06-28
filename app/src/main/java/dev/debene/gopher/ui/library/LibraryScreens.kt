package dev.debene.gopher.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    viewModel: LibraryViewModel,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Bookmarks") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
    }) { padding ->
        if (bookmarks.isEmpty()) {
            EmptyState("No bookmarks yet", Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(bookmarks, key = { it.url }) { bookmark ->
                    ListItem(
                        headlineContent = { Text(bookmark.title) },
                        supportingContent = { Text(bookmark.url, style = MaterialTheme.typography.bodySmall) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removeBookmark(bookmark.url) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove")
                            }
                        },
                        modifier = Modifier.clickable { onOpen(bookmark.url) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: LibraryViewModel,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    val history by viewModel.history.collectAsState()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("History") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.clearHistory() }) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear history")
                }
            },
        )
    }) { padding ->
        if (history.isEmpty()) {
            EmptyState("No history yet", Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(history, key = { it.id }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.title) },
                        supportingContent = { Text(entry.url, style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.clickable { onOpen(entry.url) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
    }
}
