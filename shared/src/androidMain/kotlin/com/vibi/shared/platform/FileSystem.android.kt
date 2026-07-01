package com.vibi.shared.platform

import android.net.Uri
import com.vibi.shared.data.local.db.applicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

actual fun deleteLocalFile(path: String): Boolean = runCatching {
    when {
        // content:// — SAF/MediaStore 등은 파일 핸들이 아니라 ContentResolver 로 삭제.
        path.startsWith("content://") ->
            applicationContext.contentResolver.delete(Uri.parse(path), null, null) > 0
        // file:// — URI 의 path 컴포넌트를 실제 파일 경로로 환원.
        path.startsWith("file://") ->
            Uri.parse(path).path?.let { File(it).delete() } ?: false
        else -> File(path).delete()
    }
}.getOrDefault(false)

actual fun fileExists(path: String): Boolean = runCatching {
    when {
        path.startsWith("content://") ->
            applicationContext.contentResolver.openFileDescriptor(Uri.parse(path), "r")
                ?.use { true } ?: false
        path.startsWith("file://") ->
            Uri.parse(path).path?.let { File(it).exists() } ?: false
        else -> File(path).exists()
    }
}.getOrDefault(false)

// iOS NSUUID().UUIDString() 은 대문자 — 동일 ID 표현을 위해 uppercase.
actual fun generateId(): String = UUID.randomUUID().toString().uppercase()

/**
 * iOS 의 atomically=true 와 동등한 원자적 쓰기 — 같은 디렉터리에 temp sibling 으로 쓴 뒤
 * renameTo(target) 로 교체한다. 부분 기록된 파일이 reader 에 노출되지 않게 한다.
 * 부모 디렉터리는 먼저 mkdirs 로 보장.
 */
private fun atomicWrite(target: File, write: (File) -> Unit) {
    target.parentFile?.mkdirs()
    val tmp = File(target.parentFile, "${target.name}.${UUID.randomUUID()}.tmp")
    try {
        write(tmp)
        if (!tmp.renameTo(target)) {
            // 일부 상황(기존 target 존재)에서 rename 실패 가능 — 명시적으로 교체.
            target.delete()
            if (!tmp.renameTo(target)) {
                // 최후의 수단: 직접 복사 (예: cross-filesystem rename 불가).
                tmp.copyTo(target, overwrite = true)
            }
        }
    } finally {
        if (tmp.exists()) tmp.delete()
    }
}

actual fun writeTextToFile(path: String, content: String) {
    atomicWrite(File(path)) { it.writeText(content) }
}

actual fun saveBytesToCache(fileName: String, bytes: ByteArray): String {
    val file = File(applicationContext.cacheDir, fileName)
    atomicWrite(file) { it.writeBytes(bytes) }
    return file.absolutePath
}

actual fun saveBytesToPersistentFile(fileName: String, bytes: ByteArray): String {
    // filesDir 는 cacheDir 와 달리 OS 가 임의로 비우지 않음 — 오프라인 stem 재생 보장.
    val dir = File(applicationContext.filesDir, "stems")
    val file = File(dir, fileName)
    atomicWrite(file) { it.writeBytes(bytes) }
    return file.absolutePath
}

actual suspend fun readFileBytes(uriOrPath: String): ByteArray = withContext(Dispatchers.IO) {
    if (uriOrPath.startsWith("content://") || uriOrPath.startsWith("file://")) {
        val uri = Uri.parse(uriOrPath)
        requireNotNull(applicationContext.contentResolver.openInputStream(uri)) {
            "Cannot open input stream for $uriOrPath"
        }.use { it.readBytes() }
    } else {
        File(uriOrPath).readBytes()
    }
}
