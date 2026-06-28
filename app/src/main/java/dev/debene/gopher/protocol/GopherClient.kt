package dev.debene.gopher.protocol

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.coroutines.coroutineContext

/**
 * Gopher transport. One suspend [fetch] over a TCP socket replaces the J2ME
 * `Data.fetchText` / `Data.fetchImage` pair (both did the same socket dance for different
 * payloads — here the caller decides how to interpret the bytes).
 *
 * Networking runs on [ioDispatcher]; TLS (`gophers://`) uses an [SSLSocket]. Coroutine
 * cancellation closes the socket so an in-flight request is aborted promptly, restoring the
 * old "Stop" behaviour.
 */
class GopherClient(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 30_000,
    private val sslSocketFactory: SSLSocketFactory =
        SSLSocketFactory.getDefault() as SSLSocketFactory,
) {

    @Throws(IOException::class)
    suspend fun fetch(request: GopherRequest): ByteArray = withContext(ioDispatcher) {
        val socket = if (request.tls) sslSocketFactory.createSocket() as SSLSocket else Socket()
        try {
            socket.soTimeout = readTimeoutMs
            socket.connect(InetSocketAddress(request.host, request.port), connectTimeoutMs)
            if (socket is SSLSocket) socket.startHandshake()

            // A Gopher request is just the selector followed by CRLF. Search queries already
            // carry their "\t<query>" suffix inside the selector.
            socket.getOutputStream().apply {
                write((request.selector + "\r\n").toByteArray(Charsets.UTF_8))
                flush()
            }

            readAll(socket)
        } finally {
            runCatching { socket.close() }
        }
    }

    /** Reads to EOF, checking for coroutine cancellation between chunks. */
    private suspend fun readAll(socket: Socket): ByteArray {
        val input = socket.getInputStream()
        val buffer = ByteArray(16 * 1024)
        val out = java.io.ByteArrayOutputStream(buffer.size)
        while (true) {
            coroutineContext.ensureActive() // closes-on-cancel via the finally above
            val n = input.read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }
}
