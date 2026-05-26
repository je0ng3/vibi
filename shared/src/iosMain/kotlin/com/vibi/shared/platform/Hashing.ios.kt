@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.vibi.shared.platform

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileSize
import platform.Foundation.NSDate
import platform.Foundation.NSInputStream
import platform.Foundation.NSNumber
import platform.Foundation.inputStreamWithFileAtPath
import platform.posix.uint8_tVar

private const val CHUNK_BYTES = 64 * 1024

actual suspend fun sha256HexOfFile(path: String): String = withContext(Dispatchers.Default) {
    val resolved = resolveStoredUriToPath(path) ?: path
    val stream = NSInputStream.inputStreamWithFileAtPath(resolved)
        ?: error("sha256HexOfFile: cannot open $path (resolved=$resolved)")
    stream.open()
    try {
        val hasher = Sha256()
        val buf = ByteArray(CHUNK_BYTES)
        buf.usePinned { pinned ->
            val ptr = pinned.addressOf(0).reinterpret<uint8_tVar>()
            while (true) {
                val n = stream.read(ptr, CHUNK_BYTES.toULong()).toInt()
                if (n <= 0) break
                hasher.update(buf, 0, n)
            }
        }
        hasher.digestHex()
    } finally {
        stream.close()
    }
}

actual suspend fun statFile(path: String): FileStat = withContext(Dispatchers.Default) {
    val resolved = resolveStoredUriToPath(path) ?: path
    val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(resolved, null)
        ?: error("statFile: cannot read attrs of $path (resolved=$resolved)")
    val size = (attrs[NSFileSize] as NSNumber).longLongValue
    val modDate = attrs[NSFileModificationDate] as? NSDate
    val mtime = ((modDate?.timeIntervalSince1970 ?: 0.0) * 1000.0).toLong()
    FileStat(sizeBytes = size, lastModifiedMs = mtime)
}
