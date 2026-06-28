package dev.debene.gopher.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers the cases the J2ME `Data.parseGopherURL` handled, plus the new `gophers://` scheme. */
class GopherUrlTest {

    @Test
    fun bareHost_defaultsToRootDirectory() {
        val r = GopherUrl.parse("gopher.floodgap.com")!!
        assertEquals("gopher.floodgap.com", r.host)
        assertEquals(70, r.port)
        assertEquals(GopherType.DIRECTORY, r.type)
        assertEquals("", r.selector)
        assertFalse(r.tls)
    }

    @Test
    fun hostWithPort() {
        val r = GopherUrl.parse("example.org:7070")!!
        assertEquals("example.org", r.host)
        assertEquals(7070, r.port)
    }

    @Test
    fun fullUrl_keepsSelectorLeadingSlash() {
        val r = GopherUrl.parse("gopher://gopher.floodgap.com/7/v2/vs")!!
        assertEquals("gopher.floodgap.com", r.host)
        assertEquals(70, r.port)
        assertEquals(GopherType.SEARCH, r.type) // type char is '7'
        assertEquals("/v2/vs", r.selector) // the old parser wrongly dropped the leading slash
    }

    @Test
    fun textType() {
        val r = GopherUrl.parse("gopher://example.org/0/about.txt")!!
        assertEquals(GopherType.TEXT, r.type)
        assertEquals("/about.txt", r.selector)
    }

    @Test
    fun gophersScheme_setsTls() {
        val r = GopherUrl.parse("gophers://secure.example/1/")!!
        assertTrue(r.tls)
        assertEquals("secure.example", r.host)
        assertEquals(GopherType.DIRECTORY, r.type)
    }

    @Test
    fun explicitPortWithPath() {
        val r = GopherUrl.parse("gopher://host.example:71/0/file")!!
        assertEquals(71, r.port)
        assertEquals(GopherType.TEXT, r.type)
        assertEquals("/file", r.selector)
    }

    @Test
    fun blankAndSchemeOnly_returnNull() {
        assertNull(GopherUrl.parse(""))
        assertNull(GopherUrl.parse("   "))
        assertNull(GopherUrl.parse("gopher://"))
    }

    @Test
    fun roundTrip_formatThenParse() {
        val original = GopherRequest("gopher.floodgap.com", 70, GopherType.DIRECTORY, "/v2")
        val reparsed = GopherUrl.parse(original.url)!!
        assertEquals(original.host, reparsed.host)
        assertEquals(original.port, reparsed.port)
        assertEquals(original.type, reparsed.type)
        assertEquals(original.selector, reparsed.selector)
    }
}
