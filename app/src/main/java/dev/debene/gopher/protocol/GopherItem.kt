package dev.debene.gopher.protocol

/**
 * A single line in a Gopher menu. Ports the data half of the J2ME `DirectoryItem`
 * (the UI half — `StringItem` rendering — is now handled by Compose).
 *
 * @param rawCode the original type character from the wire, preserved even when it maps
 *                to [GopherType.UNKNOWN], so the UI can still show something meaningful.
 */
data class GopherItem(
    val type: GopherType,
    val display: String,
    val selector: String = "",
    val host: String = "",
    val port: Int = GopherRequest.DEFAULT_PORT,
    val rawCode: Char = type.code,
) {
    val isSelectable: Boolean get() = type.isSelectable && (host.isNotEmpty() || type.kind == GopherType.Kind.HTML)

    /**
     * Builds the request used to open this item.
     *
     * @param tls inherited from the parent menu's connection — Gopher menus don't encode
     *            TLS, so a TLS-fetched directory propagates it to its children.
     */
    fun toRequest(tls: Boolean = false): GopherRequest =
        GopherRequest(host = host, port = port, type = type, selector = selector, tls = tls)
}
