package com.vibi.shared.platform

import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.vibi.shared.data.local.db.applicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val CHUNK_BYTES = 64 * 1024

actual suspend fun sha256HexOfFile(path: String): String = withContext(Dispatchers.IO) {
    openInputFor(path).use { stream ->
        val hasher = Sha256()
        val buf = ByteArray(CHUNK_BYTES)
        while (true) {
            val n = stream.read(buf)
            if (n < 0) break
            if (n > 0) hasher.update(buf, 0, n)
        }
        hasher.digestHex()
    }
}

actual suspend fun statFile(path: String): FileStat = withContext(Dispatchers.IO) {
    when {
        path.startsWith("content://") -> {
            val uri = Uri.parse(path)
            applicationContext.contentResolver
                .query(uri, null, null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                        val lmIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                        val lastModified = if (lmIdx >= 0 && !cursor.isNull(lmIdx)) cursor.getLong(lmIdx) else 0L
                        FileStat(sizeBytes = size, lastModifiedMs = lastModified)
                    } else {
                        FileStat(sizeBytes = 0L, lastModifiedMs = 0L)
                    }
                } ?: FileStat(sizeBytes = 0L, lastModifiedMs = 0L)
        }
        else -> {
            val file = File(stripFileScheme(path))
            FileStat(sizeBytes = file.length(), lastModifiedMs = file.lastModified())
        }
    }
}
