package dev.debene.gopher.protocol

/**
 * Parses a Gopher menu (directory listing) into [GopherItem]s.
 *
 * Ports `Data.parseDirectory` / `Data.parseDirectoryItem`. Each line is
 * `<type><display>\t<selector>\t<host>\t<port>`; the menu is terminated by a lone `.`.
 */
object GopherParser {

    fun parseMenu(text: String): List<GopherItem> {
        if (text.isEmpty()) return emptyList()
        val items = ArrayList<GopherItem>()
        // Lines are CRLF-delimited on the wire; tolerate lone LF and trailing CR.
        for (raw in text.split('\n')) {
            val line = raw.trimEnd('\r')
            if (line.isEmpty()) continue
            if (line == ".") break // menu terminator
            parseLine(line)?.let(items::add)
        }
        return items
    }

    private fun parseLine(line: String): GopherItem? {
        val typeChar = line[0]
        val type = GopherType.fromCode(typeChar)
        val fields = line.substring(1).split('\t')
        val display = fields.getOrElse(0) { "" }

        // Info / error lines, and malformed lines, carry only a label.
        if (type == GopherType.INFO || type == GopherType.ERROR || fields.size < 2) {
            return GopherItem(type, display, rawCode = typeChar)
        }

        var selector = fields[1]
        // 'h' web links encode the target as "URL:http://..." in the selector field.
        if (type == GopherType.HTML && selector.startsWith("URL:")) {
            selector = selector.substring(4)
        }
        val host = fields.getOrElse(2) { "" }
        val port = fields.getOrElse(3) { "" }.toIntOrNull() ?: GopherRequest.DEFAULT_PORT

        return GopherItem(type, display, selector, host, port, rawCode = typeChar)
    }
}
