package dev.debene.gopher.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers `Data.parseDirectory` / `parseDirectoryItem` behaviour: types, fields, info/error
 *  lines, the trailing `.`, the `h` URL: case, and malformed lines. */
class GopherParserTest {

    private fun line(vararg fields: String) = fields.joinToString("\t")

    @Test
    fun parsesTypesHostsAndPorts() {
        val menu = buildString {
            append(line("1Floodgap", "/", "gopher.floodgap.com", "70")).append("\r\n")
            append(line("0About", "/about.txt", "gopher.floodgap.com", "70")).append("\r\n")
            append(".").append("\r\n")
        }
        val items = GopherParser.parseMenu(menu)
        assertEquals(2, items.size)
        assertEquals(GopherType.DIRECTORY, items[0].type)
        assertEquals("Floodgap", items[0].display)
        assertEquals("gopher.floodgap.com", items[0].host)
        assertEquals(70, items[0].port)
        assertEquals(GopherType.TEXT, items[1].type)
        assertEquals("/about.txt", items[1].selector)
    }

    @Test
    fun infoAndErrorLinesCarryOnlyText() {
        val menu = "iWelcome\tfake\t(NULL)\t0\r\n3Something broke\terr\thost\t70\r\n.\r\n"
        val items = GopherParser.parseMenu(menu)
        assertEquals(2, items.size)
        assertEquals(GopherType.INFO, items[0].type)
        assertEquals("Welcome", items[0].display)
        assertEquals("", items[0].host) // info lines drop selector/host/port
        assertEquals(GopherType.ERROR, items[1].type)
        assertTrue(items.none { it.type == GopherType.UNKNOWN })
    }

    @Test
    fun htmlUrlPrefixIsStripped() {
        val menu = "hExample\tURL:https://example.com\thost\t70\r\n.\r\n"
        val item = GopherParser.parseMenu(menu).single()
        assertEquals(GopherType.HTML, item.type)
        assertEquals("https://example.com", item.selector)
    }

    @Test
    fun terminatorAndBlankLinesAreIgnored() {
        val menu = "1Dir\t/\thost\t70\r\n\r\n.\r\ntrailing garbage after dot\r\n"
        val items = GopherParser.parseMenu(menu)
        assertEquals(1, items.size) // parsing stops at the lone "."
    }

    @Test
    fun malformedShortLineKeepsTypeAndLabel() {
        val items = GopherParser.parseMenu("0JustALabel\r\n")
        val item = items.single()
        assertEquals(GopherType.TEXT, item.type)
        assertEquals("JustALabel", item.display)
        assertEquals("", item.selector)
    }

    @Test
    fun badPortFallsBackToSeventy() {
        val item = GopherParser.parseMenu("1Dir\t/\thost\tNaN\r\n").single()
        assertEquals(70, item.port)
    }

    @Test
    fun emptyInputYieldsEmptyList() {
        assertTrue(GopherParser.parseMenu("").isEmpty())
    }
}
