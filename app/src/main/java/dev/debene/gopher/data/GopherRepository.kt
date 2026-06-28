package dev.debene.gopher.data

import dev.debene.gopher.data.db.CacheDao
import dev.debene.gopher.data.db.CacheEntity
import dev.debene.gopher.protocol.GopherClient
import dev.debene.gopher.protocol.GopherRequest
import dev.debene.gopher.protocol.GopherType

/**
 * Fetches Gopher resources, transparently caching cacheable bodies (menus and text) so a
 * revisit within the TTL skips the network — the "caching" feature from the plan.
 */
class GopherRepository(
    private val client: GopherClient,
    private val cacheDao: CacheDao,
    private val cacheTtlMs: Long = 10 * 60 * 1000L,
    private val maxCacheBytes: Int = 512 * 1024,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun fetch(request: GopherRequest, forceReload: Boolean = false): ByteArray {
        val url = request.url
        val cacheable = request.type.kind == GopherType.Kind.MENU ||
            request.type.kind == GopherType.Kind.TEXT

        if (cacheable && !forceReload) {
            cacheDao.get(url)?.let { cached ->
                if (now() - cached.fetchedAt < cacheTtlMs) return cached.body
            }
        }

        val body = client.fetch(request)

        if (cacheable && body.size <= maxCacheBytes) {
            cacheDao.put(CacheEntity(url, body, now()))
        }
        return body
    }

    suspend fun clearCache() = cacheDao.clear()
}
