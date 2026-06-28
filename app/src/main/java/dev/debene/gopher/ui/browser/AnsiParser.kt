package dev.debene.gopher.ui.browser

/**
 * Minimal ANSI/VT100 SGR parser: turns text containing `ESC[…m` color codes into styled
 * [AnsiRun]s, and strips other escape sequences (cursor moves, screen clears, OSC).
 *
 * Pure Kotlin (colors are ARGB [Int]s, no Compose types) so it can be unit-tested headlessly;
 * the Compose [TextViewer] maps runs to an AnnotatedString.
 */
data class AnsiRun(
    val text: String,
    val fg: Int? = null,
    val bg: Int? = null,
    val bold: Boolean = false,
)

object AnsiParser {

    fun hasAnsi(text: String): Boolean = text.indexOf('\u001B') >= 0

    fun parse(text: String): List<AnsiRun> {
        val runs = ArrayList<AnsiRun>()
        val buf = StringBuilder()
        var fg: Int? = null
        var bg: Int? = null
        var bold = false

        fun flush() {
            if (buf.isNotEmpty()) {
                runs.add(AnsiRun(buf.toString(), fg, bg, bold))
                buf.setLength(0)
            }
        }

        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            if (c == '\u001B' && i + 1 < n && text[i + 1] == '[') {
                // CSI sequence: ESC [ params... finalByte (0x40-0x7E)
                var j = i + 2
                while (j < n && text[j] !in '@'..'~') j++
                if (j >= n) break // malformed/truncated
                val finalByte = text[j]
                if (finalByte == 'm') {
                    flush()
                    val params = text.substring(i + 2, j)
                    val (nf, nb, nbold) = applySgr(params, fg, bg, bold)
                    fg = nf; bg = nb; bold = nbold
                }
                // any other final byte (J, H, K, A…) is a non-color control: drop it
                i = j + 1
            } else if (c == '\u001B') {
                // Other escape (OSC "ESC ]", charset "ESC (", etc.): skip the ESC and,
                // for the common 2-char forms, the following byte.
                i += if (i + 1 < n) 2 else 1
            } else {
                buf.append(c)
                i++
            }
        }
        flush()
        return runs
    }

    private data class State(val fg: Int?, val bg: Int?, val bold: Boolean)

    private fun applySgr(params: String, fg0: Int?, bg0: Int?, bold0: Boolean): State {
        if (params.isEmpty()) return State(null, null, false) // ESC[m == reset
        val codes = params.split(';').map { it.toIntOrNull() ?: 0 }
        var fg = fg0; var bg = bg0; var bold = bold0
        var k = 0
        while (k < codes.size) {
            when (val code = codes[k]) {
                0 -> { fg = null; bg = null; bold = false }
                1 -> bold = true
                22 -> bold = false
                in 30..37 -> fg = BASIC[code - 30]
                in 90..97 -> fg = BASIC[code - 90 + 8]
                39 -> fg = null
                in 40..47 -> bg = BASIC[code - 40]
                in 100..107 -> bg = BASIC[code - 100 + 8]
                49 -> bg = null
                38, 48 -> {
                    val isFg = code == 38
                    when (codes.getOrNull(k + 1)) {
                        5 -> { // 256-color: 38;5;n
                            val v = xterm256(codes.getOrNull(k + 2) ?: 0)
                            if (isFg) fg = v else bg = v
                            k += 2
                        }
                        2 -> { // truecolor: 38;2;r;g;b
                            val r = codes.getOrNull(k + 2) ?: 0
                            val g = codes.getOrNull(k + 3) ?: 0
                            val b = codes.getOrNull(k + 4) ?: 0
                            val v = argb(r, g, b)
                            if (isFg) fg = v else bg = v
                            k += 4
                        }
                    }
                }
                // other SGR (italic, underline, etc.) ignored
            }
            k++
        }
        return State(fg, bg, bold)
    }

    private fun argb(r: Int, g: Int, b: Int): Int =
        (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)

    /** Standard 16 ANSI colors (xterm). */
    private val BASIC = intArrayOf(
        argb(0, 0, 0), argb(205, 0, 0), argb(0, 205, 0), argb(205, 205, 0),
        argb(0, 0, 238), argb(205, 0, 205), argb(0, 205, 205), argb(229, 229, 229),
        argb(127, 127, 127), argb(255, 0, 0), argb(0, 255, 0), argb(255, 255, 0),
        argb(92, 92, 255), argb(255, 0, 255), argb(0, 255, 255), argb(255, 255, 255),
    )

    fun xterm256(n: Int): Int = when {
        n < 16 -> BASIC[n.coerceIn(0, 15)]
        n in 16..231 -> {
            val c = n - 16
            fun v(x: Int) = if (x == 0) 0 else 55 + x * 40
            argb(v((c / 36) % 6), v((c / 6) % 6), v(c % 6))
        }
        else -> {
            val gray = 8 + (n.coerceIn(232, 255) - 232) * 10
            argb(gray, gray, gray)
        }
    }
}
