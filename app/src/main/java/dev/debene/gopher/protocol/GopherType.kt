package dev.debene.gopher.protocol

/**
 * Gopher item types as defined by RFC 1436 plus the common Gopher+ / de-facto
 * extensions (`g`, `h`, `i`, `I`, `s`, `d`, ...).
 *
 * Ported from the J2ME `DirectoryItem.setProperLabel()` switch, extended with the
 * remaining standard types and a behavioural [kind] used by the UI to decide how to
 * open an item.
 */
enum class GopherType(val code: Char, val label: String, val kind: Kind) {
    TEXT('0', "TXT", Kind.TEXT),
    DIRECTORY('1', "DIR", Kind.MENU),
    CSO('2', "CSO", Kind.TELNET),
    ERROR('3', "ERR", Kind.ERROR),
    BINHEX('4', "HQX", Kind.BINARY),
    DOS_BINARY('5', "BIN", Kind.BINARY),
    UUENCODED('6', "UUE", Kind.BINARY),
    SEARCH('7', "QRY", Kind.SEARCH),
    TELNET('8', "TEL", Kind.TELNET),
    BINARY('9', "BIN", Kind.BINARY),
    REDUNDANT('+', "DUP", Kind.MENU),
    TN3270('T', "TEL", Kind.TELNET),
    GIF('g', "GIF", Kind.IMAGE),
    IMAGE('I', "IMG", Kind.IMAGE),
    HTML('h', "WWW", Kind.HTML),
    INFO('i', "", Kind.INFO),
    SOUND('s', "SND", Kind.BINARY),
    DOCUMENT('d', "DOC", Kind.BINARY),
    PDF('P', "PDF", Kind.BINARY),
    XML('X', "XML", Kind.TEXT),
    UNKNOWN('?', "???", Kind.BINARY);

    /** Whether this entry is actionable (not an info line or error marker). */
    val isSelectable: Boolean get() = kind != Kind.INFO && kind != Kind.ERROR

    enum class Kind { MENU, TEXT, IMAGE, SEARCH, HTML, BINARY, TELNET, INFO, ERROR }

    companion object {
        private val byCode = entries.associateBy { it.code }

        /** Returns the matching type, or [UNKNOWN] for unrecognised codes. */
        fun fromCode(code: Char): GopherType = byCode[code] ?: UNKNOWN
    }
}
