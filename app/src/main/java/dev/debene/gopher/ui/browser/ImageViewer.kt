package dev.debene.gopher.ui.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import java.nio.ByteBuffer

/** Displays an image item (types `g`/`I`) decoded from the fetched bytes via Coil. */
@Composable
fun ImageViewer(bytes: ByteArray, contentDescription: String?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(
            // Coil decodes a ByteBuffer via its built-in fetcher.
            model = ImageRequest.Builder(LocalContext.current)
                .data(ByteBuffer.wrap(bytes))
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        )
    }
}
