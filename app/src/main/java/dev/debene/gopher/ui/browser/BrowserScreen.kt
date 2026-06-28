package dev.debene.gopher.ui.browser

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import dev.debene.gopher.protocol.GopherItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var address by remember(state.addressText) { mutableStateOf(state.addressText) }
    var overflowOpen by remember { mutableStateOf(false) }
    var searchTarget by remember { mutableStateOf<GopherItem?>(null) }

    // React to one-shot events.
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is BrowserEvent.OpenWebUrl -> {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(event.url)))
                    } catch (e: ActivityNotFoundException) {
                        snackbarHostState.showSnackbar("No app can open ${event.url}")
                    }
                }
                is BrowserEvent.ShowSearchDialog -> searchTarget = event.item
                is BrowserEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("gopher://…") },
                        trailingIcon = {
                            if (address.isNotEmpty()) {
                                IconButton(onClick = { address = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            focusManager.clearFocus()
                            if (address.isNotBlank()) viewModel.navigateToAddress(address)
                        }),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.goBack() }, enabled = state.canGoBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.goHome() }) {
                        Icon(Icons.Filled.Home, contentDescription = "Home")
                    }
                    IconButton(
                        onClick = { viewModel.toggleBookmark() },
                        enabled = state.current != null,
                    ) {
                        Icon(
                            imageVector = if (state.isBookmarked) Icons.Filled.Bookmark
                            else Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark",
                        )
                    }
                    IconButton(onClick = { overflowOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Bookmarks") },
                            leadingIcon = { Icon(Icons.Filled.Bookmarks, null) },
                            onClick = { overflowOpen = false; onOpenBookmarks() },
                        )
                        DropdownMenuItem(
                            text = { Text("History") },
                            leadingIcon = { Icon(Icons.Filled.History, null) },
                            onClick = { overflowOpen = false; onOpenHistory() },
                        )
                        DropdownMenuItem(
                            text = { Text("Reload") },
                            leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                            onClick = { overflowOpen = false; viewModel.reload() },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
            when (val content = state.content) {
                is PageContent.Menu -> MenuList(content.items, onItemClick = viewModel::open)
                is PageContent.Text -> TextViewer(content.text)
                is PageContent.Image -> ImageViewer(content.bytes, contentDescription = state.title)
                PageContent.Blank -> if (state.isLoading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
        }
    }

    searchTarget?.let { target ->
        SearchDialog(
            title = target.display.trim().ifBlank { "Query" },
            onSubmit = {
                searchTarget = null
                viewModel.submitSearch(target, it)
            },
            onDismiss = { searchTarget = null },
        )
    }
}
