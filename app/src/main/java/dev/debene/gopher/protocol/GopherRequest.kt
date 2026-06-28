package dev.debene.gopher.protocol

/**
 * An addressable Gopher resource: where to connect ([host]/[port]/[tls]), what selector
 * to send, and the expected [type] of the response (so the UI knows how to render it).
 *
 * Replaces the ad-hoc `hostname`/`port`/`selector` fields the J2ME `DirectoryItem` carried.
 */
data class GopherRequest(
    val host: String,
    val port: Int = DEFAULT_PORT,
    val type: GopherType = GopherType.DIRECTORY,
    val selector: String = "",
    val tls: Boolean = false,
) {
    /** Canonical `gopher://` (or `gophers://`) URL for this request. */
    val url: String get() = GopherUrl.format(this)

    /** Human-friendly "host port selector" title, mirroring the old title format. */
    val title: String
        get() = buildString {
            append(host)
            if (port != DEFAULT_PORT) append(':').append(port)
            if (selector.isNotEmpty()) append(' ').append(selector.substringBefore('\t'))
        }

    companion object {
        const val DEFAULT_PORT = 70
    }
}
