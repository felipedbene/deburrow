package dev.debene.gopher.protocol

/**
 * Parses and formats `gopher://` / `gophers://` URLs.
 *
 * Ports `Data.parseGopherURL` (originally based on code by Nuno J. Silva) and corrects its
 * one quirk: the old parser dropped the leading slash of the selector. The canonical form is
 * `gopher://host[:port]/<type><selector>`, where `<selector>` keeps its own leading slash.
 */
object GopherUrl {

    /** Returns a request, or `null` if [input] has no usable host. */
    fun parse(input: String): GopherRequest? {
        var s = input.trim()
        if (s.isEmpty()) return null

        var tls = false
        when {
            s.startsWith("gophers://", ignoreCase = true) -> { tls = true; s = s.substring(10) }
            s.startsWith("gopher://", ignoreCase = true) -> s = s.substring(9)
        }
        if (s.isEmpty()) return null

        val slash = s.indexOf('/')
        val authority = if (slash == -1) s else s.substring(0, slash)
        val path = if (slash == -1) "" else s.substring(slash + 1)

        val colon = authority.indexOf(':')
        val host: String
        var port = GopherRequest.DEFAULT_PORT
        if (colon != -1) {
            host = authority.substring(0, colon)
            port = authority.substring(colon + 1).toIntOrNull() ?: GopherRequest.DEFAULT_PORT
        } else {
            host = authority
        }
        if (host.isEmpty()) return null

        var type = GopherType.DIRECTORY
        var selector = ""
        if (path.isNotEmpty()) {
            type = GopherType.fromCode(path[0])
            selector = path.substring(1) // keep the selector's own leading slash, if any
        }

        return GopherRequest(host, port, type, selector, tls)
    }

    /** Renders [request] back to a canonical URL string. */
    fun format(request: GopherRequest): String = buildString {
        append(if (request.tls) "gophers://" else "gopher://")
        append(request.host)
        if (request.port != GopherRequest.DEFAULT_PORT) append(':').append(request.port)
        append('/').append(request.type.code)
        append(request.selector)
    }
}
