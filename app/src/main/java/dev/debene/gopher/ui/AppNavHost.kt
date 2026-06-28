package dev.debene.gopher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.debene.gopher.di.AppContainer
import dev.debene.gopher.ui.browser.BrowserScreen
import dev.debene.gopher.ui.browser.BrowserViewModel
import dev.debene.gopher.ui.library.BookmarksScreen
import dev.debene.gopher.ui.library.HistoryScreen
import dev.debene.gopher.ui.library.LibraryViewModel

private object Routes {
    const val BROWSER = "browser"
    const val BOOKMARKS = "bookmarks"
    const val HISTORY = "history"
}

@Composable
fun AppNavHost(
    container: AppContainer,
    startUrl: String? = null,
    deepLinks: Flow<String> = emptyFlow(),
) {
    val navController = rememberNavController()

    // Both ViewModels are hosted at the Activity scope (this composable's store owner),
    // so the browser keeps its state while the user dips into bookmarks/history.
    val browserViewModel: BrowserViewModel = viewModel(factory = BrowserViewModel.factory(container))
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(container))

    // Open the incoming gopher:// deep link once, after the home page has loaded.
    LaunchedEffect(startUrl) {
        if (!startUrl.isNullOrBlank()) browserViewModel.navigateToAddress(startUrl)
    }
    // And handle links that arrive while the app is already running (onNewIntent).
    LaunchedEffect(Unit) {
        deepLinks.collect { url -> browserViewModel.navigateToAddress(url) }
    }

    NavHost(navController = navController, startDestination = Routes.BROWSER) {
        composable(Routes.BROWSER) {
            BrowserScreen(
                viewModel = browserViewModel,
                onOpenBookmarks = { navController.navigate(Routes.BOOKMARKS) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
            )
        }
        composable(Routes.BOOKMARKS) {
            BookmarksScreen(
                viewModel = libraryViewModel,
                onOpen = { url ->
                    browserViewModel.navigateToAddress(url)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                viewModel = libraryViewModel,
                onOpen = { url ->
                    browserViewModel.navigateToAddress(url)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
