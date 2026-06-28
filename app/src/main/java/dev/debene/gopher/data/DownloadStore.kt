package dev.debene.gopher.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.debene.gopher.protocol.GopherItem
import java.io.File

/**
 * Saves binary Gopher items to the device's public Downloads directory using scoped storage
 * (MediaStore on API 29+, a direct file write on older versions). Implements the "downloads"
 * feature that replaces the old "Unsupported item type" alert.
 */
class DownloadStore(private val context: Context) {

    /** Returns the saved file's [Uri], or throws on I/O failure. */
    suspend fun save(item: GopherItem, bytes: ByteArray): Uri = withContext(Dispatchers.IO) {
        val fileName = deriveFileName(item)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(fileName, bytes)
        } else {
            saveLegacy(fileName, bytes)
        }
    }

    private fun saveViaMediaStore(fileName: String, bytes: ByteArray): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values)
            ?: throw java.io.IOException("Could not create download entry")
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: throw java.io.IOException("Could not open output stream")
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(fileName: String, bytes: ByteArray): Uri {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        return Uri.fromFile(file)
    }

    /** Derives a sensible filename from the selector's last path segment. */
    private fun deriveFileName(item: GopherItem): String {
        val fromSelector = item.selector
            .substringBefore('\t')
            .trimEnd('/')
            .substringAfterLast('/')
            .ifBlank { item.display.trim() }
            .ifBlank { "download" }
        return fromSelector.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}
