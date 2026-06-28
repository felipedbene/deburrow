package dev.debene.gopher.ui.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnsiParserTest {

    private fun argb(r: Int, g: Int, b: Int) =
        (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    @Test fun plainTextIsOneUnstyledRun() {
        val runs = AnsiParser.parse("hello world")
        assertEquals(1, runs.size)
        assertEquals("hello world", runs[0].text)
        assertNull(runs[0].fg)
        assertNull(runs[0].bg)
        assertFalse(runs[0].bold)
    }

    @Test fun basicForegroundColor() {
        val runs = AnsiParser.parse("\u001B[31mRED\u001B[0m").filter { it.text.isNotEmpty() }
        assertEquals("RED", runs[0].text)
        assertEquals(argb(205, 0, 0), runs[0].fg)
    }

    @Test fun boldAndBrightGreen() {
        val runs = AnsiParser.parse("\u001B[1;92mok").filter { it.text.isNotEmpty() }
        assertEquals("ok", runs[0].text)
        assertTrue(runs[0].bold)
        assertEquals(argb(0, 255, 0), runs[0].fg) // bright green (90+2 -> index 10)
    }

    @Test fun background256Color() {
        val runs = AnsiParser.parse("\u001B[48;5;21m \u001B[49m").filter { it.text == " " }
        // 256-color index 21 = cube (0,0,5) -> blue
        assertEquals(argb(0, 0, 255), runs[0].bg)
    }

    @Test fun truecolor() {
        val runs = AnsiParser.parse("\u001B[38;2;10;20;30mZ").filter { it.text.isNotEmpty() }
        assertEquals(argb(10, 20, 30), runs[0].fg)
    }

    @Test fun nonColorEscapesAreStripped() {
        // ESC[2J (clear screen) and ESC[H (cursor home) must vanish, leaving clean text.
        val runs = AnsiParser.parse("before\u001B[2J\u001B[Hafter")
        assertEquals("beforeafter", runs.joinToString("") { it.text })
    }

    @Test fun resetClearsStyle() {
        val runs = AnsiParser.parse("\u001B[31mA\u001B[0mB").filter { it.text.isNotEmpty() }
        assertEquals(argb(205, 0, 0), runs.first { it.text == "A" }.fg)
        assertNull(runs.first { it.text == "B" }.fg)
    }

    @Test fun hasAnsiDetection() {
        assertTrue(AnsiParser.hasAnsi("x\u001B[31my"))
        assertFalse(AnsiParser.hasAnsi("plain text"))
    }
}
