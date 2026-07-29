package dev.debene.gopher.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Renders a plain-text or ANSI Gopher file in a monospace viewport.
 *
 * - Long lines **wrap** by default (readable prose). A toggle switches to no-wrap +
 *   horizontal scroll for fixed-width ASCII art / maps.
 * - **ANSI** color codes (`ESC[…m`) in `.ansi` files are parsed into colored text instead
 *   of showing as escape-sequence gibberish. ANSI pages render on a forced black backdrop
 *   (see [AnsiBackground]) rather than the Material surface.
 */
@Composable
fun TextViewer(text: String, modifier: Modifier = Modifier) {
    val isAnsi = remember(text) { AnsiParser.hasAnsi(text) }
    // ANSI art is usually fixed-width, so default it to no-wrap; prose wraps.
    var wrap by remember(text) { mutableStateOf(!isAnsi) }

    val annotated = remember(text) {
        buildAnnotatedString {
            for (run in AnsiParser.parse(text)) {
                if (run.fg == null && run.bg == null && !run.bold) {
                    append(run.text)
                } else {
                    withStyle(
                        SpanStyle(
                            color = run.fg?.let { Color(it) } ?: Color.Unspecified,
                            background = run.bg?.let { Color(it) } ?: Color.Unspecified,
                            fontWeight = if (run.bold) FontWeight.Bold else null,
                        )
                    ) { append(run.text) }
                }
            }
        }
    }

    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    Column(
        modifier
            .fillMaxSize()
            .then(if (isAnsi) Modifier.background(AnsiBackground) else Modifier)
    ) {
        FilledIconToggleButton(
            checked = wrap,
            onCheckedChange = { wrap = it },
            modifier = Modifier.align(Alignment.End).padding(end = 8.dp, top = 4.dp),
        ) {
            Icon(
                imageVector = if (wrap) Icons.AutoMirrored.Filled.WrapText else Icons.Filled.MoreHoriz,
                contentDescription = if (wrap) "Wrapping on — tap for no-wrap" else "No-wrap — tap to wrap",
            )
        }

        val content = Modifier
            .fillMaxSize()
            .verticalScroll(vScroll)
            .let { if (wrap) it else it.horizontalScroll(hScroll) }
            .padding(horizontal = 12.dp, vertical = 4.dp)

        Text(
            text = annotated,
            color = if (isAnsi) AnsiForeground else Color.Unspecified,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            softWrap = wrap,
            modifier = content,
        )
    }
}

/**
 * ANSI art is authored for a black terminal, and [AnsiParser] emits the fixed xterm palette
 * (unaware of the app theme). Painting that onto the light Material surface makes the light end
 * of the palette — grey `argb(229,229,229)` and white `argb(255,255,255)` — near-invisible, so
 * ANSI pages get the background their colors were chosen against. Pure black matches the
 * palette's own color 0, so `ESC[40m` fills blend instead of showing as boxes.
 */
private val AnsiBackground = Color(0xFF000000)

/** Terminal default foreground (xterm color 7) for runs with no explicit SGR color. */
private val AnsiForeground = Color(0xFFE5E5E5)
