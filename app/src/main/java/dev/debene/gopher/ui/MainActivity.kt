package dev.debene.gopher.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.flow.MutableSharedFlow
import dev.debene.gopher.GopherApp
import dev.debene.gopher.ui.theme.GopherTheme

class MainActivity : ComponentActivity() {

    // Emits gopher:// URLs arriving via onNewIntent while the app is already running.
    private val deepLinks = MutableSharedFlow<String>(extraBufferCapacity = 4)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GopherApp).container
        val startUrl = intent?.data?.toString()
        setContent {
            GopherTheme {
                AppNavHost(container, startUrl = startUrl, deepLinks = deepLinks)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.toString()?.let { deepLinks.tryEmit(it) }
    }
}
