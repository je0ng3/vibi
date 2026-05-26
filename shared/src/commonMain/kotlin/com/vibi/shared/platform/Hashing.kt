package com.vibi.shared.platform

/**
 * 파일의 SHA-256 hash 를 lowercase hex 로 반환.
 *
 * v3 asset-by-reference 흐름에서 R2 dedup 키로 사용. 큰 파일도 메모리 폭주 없이 처리할
 * 수 있도록 platform 마다 streaming 방식으로 구현 ([Sha256] update/digest 패턴).
 *
 * iosMain: NSInputStream 으로 chunk 단위 read.
 * androidMain: 미구현 (NotImplementedError) — v3 흐름 iOS-only.
 */
expect suspend fun sha256HexOfFile(path: String): String

/**
 * 순수 Kotlin streaming SHA-256. FIPS 180-4. platform 의존 0 — actual 의 chunk reader 가
 * 이 클래스만 call 하면 됨.
 *
 * 사용:
 * ```
 * val h = Sha256()
 * while (read more) h.update(buf, 0, n)
 * val hex = h.digestHex()
 * ```
 *
 * 한 인스턴스는 [digestHex] 호출 후 더 update 불가 — 새 인스턴스 사용.
 */
class Sha256 {
    private val h = intArrayOf(
        0x6a09e667.toInt(), 0xbb67ae85.toInt(), 0x3c6ef372.toInt(), 0xa54ff53a.toInt(),
        0x510e527f.toInt(), 0x9b05688c.toInt(), 0x1f83d9ab.toInt(), 0x5be0cd19.toInt(),
    )
    private val buffer = ByteArray(64)
    private var bufferLen = 0
    private var totalBytes = 0L
    private var finalized = false

    fun update(data: ByteArray, offset: Int = 0, length: Int = data.size - offset) {
        check(!finalized) { "Sha256 already finalized" }
        require(offset >= 0 && length >= 0 && offset + length <= data.size) { "offset/length out of range" }
        totalBytes += length
        var pos = offset
        var remaining = length
        if (bufferLen > 0) {
            val need = 64 - bufferLen
            val copy = if (remaining < need) remaining else need
            data.copyInto(buffer, bufferLen, pos, pos + copy)
            bufferLen += copy
            pos += copy
            remaining -= copy
            if (bufferLen == 64) {
                processBlock(buffer, 0)
                bufferLen = 0
            }
        }
        while (remaining >= 64) {
            processBlock(data, pos)
            pos += 64
            remaining -= 64
        }
        if (remaining > 0) {
            data.copyInto(buffer, 0, pos, pos + remaining)
            bufferLen = remaining
        }
    }

    fun digestHex(): String {
        check(!finalized) { "Sha256 already finalized" }
        finalized = true
        val bitLen = totalBytes * 8L
        buffer[bufferLen++] = 0x80.toByte()
        if (bufferLen > 56) {
            while (bufferLen < 64) buffer[bufferLen++] = 0
            processBlock(buffer, 0)
            bufferLen = 0
        }
        while (bufferLen < 56) buffer[bufferLen++] = 0
        for (i in 7 downTo 0) {
            buffer[bufferLen++] = (bitLen ushr (i * 8) and 0xff).toByte()
        }
        processBlock(buffer, 0)
        val out = StringBuilder(64)
        for (word in h) {
            for (i in 3 downTo 0) {
                val b = (word ushr (i * 8)) and 0xff
                out.append(HEX[(b ushr 4) and 0xf])
                out.append(HEX[b and 0xf])
            }
        }
        return out.toString()
    }

    private fun processBlock(data: ByteArray, offset: Int) {
        val w = IntArray(64)
        for (i in 0 until 16) {
            val o = offset + i * 4
            w[i] = ((data[o].toInt() and 0xff) shl 24) or
                ((data[o + 1].toInt() and 0xff) shl 16) or
                ((data[o + 2].toInt() and 0xff) shl 8) or
                (data[o + 3].toInt() and 0xff)
        }
        for (i in 16 until 64) {
            val s0 = w[i - 15].rotateRight(7) xor w[i - 15].rotateRight(18) xor (w[i - 15] ushr 3)
            val s1 = w[i - 2].rotateRight(17) xor w[i - 2].rotateRight(19) xor (w[i - 2] ushr 10)
            w[i] = w[i - 16] + s0 + w[i - 7] + s1
        }
        var a = h[0]; var b = h[1]; var c = h[2]; var d = h[3]
        var e = h[4]; var f = h[5]; var g = h[6]; var hh = h[7]
        for (i in 0 until 64) {
            val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
            val ch = (e and f) xor (e.inv() and g)
            val t1 = hh + s1 + ch + K[i] + w[i]
            val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
            val maj = (a and b) xor (a and c) xor (b and c)
            val t2 = s0 + maj
            hh = g; g = f; f = e; e = d + t1
            d = c; c = b; b = a; a = t1 + t2
        }
        h[0] += a; h[1] += b; h[2] += c; h[3] += d
        h[4] += e; h[5] += f; h[6] += g; h[7] += hh
    }

    companion object {
        private val K = intArrayOf(
            0x428a2f98.toInt(), 0x71374491.toInt(), 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(),
            0x3956c25b.toInt(), 0x59f111f1.toInt(), 0x923f82a4.toInt(), 0xab1c5ed5.toInt(),
            0xd807aa98.toInt(), 0x12835b01.toInt(), 0x243185be.toInt(), 0x550c7dc3.toInt(),
            0x72be5d74.toInt(), 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(),
            0xe49b69c1.toInt(), 0xefbe4786.toInt(), 0x0fc19dc6.toInt(), 0x240ca1cc.toInt(),
            0x2de92c6f.toInt(), 0x4a7484aa.toInt(), 0x5cb0a9dc.toInt(), 0x76f988da.toInt(),
            0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(),
            0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351.toInt(), 0x14292967.toInt(),
            0x27b70a85.toInt(), 0x2e1b2138.toInt(), 0x4d2c6dfc.toInt(), 0x53380d13.toInt(),
            0x650a7354.toInt(), 0x766a0abb.toInt(), 0x81c2c92e.toInt(), 0x92722c85.toInt(),
            0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(),
            0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070.toInt(),
            0x19a4c116.toInt(), 0x1e376c08.toInt(), 0x2748774c.toInt(), 0x34b0bcb5.toInt(),
            0x391c0cb3.toInt(), 0x4ed8aa4a.toInt(), 0x5b9cca4f.toInt(), 0x682e6ff3.toInt(),
            0x748f82ee.toInt(), 0x78a5636f.toInt(), 0x84c87814.toInt(), 0x8cc70208.toInt(),
            0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt(),
        )
        private val HEX = "0123456789abcdef".toCharArray()
    }
}

/**
 * 로컬 파일의 (size, lastModifiedMs) 메타. Asset 캐시 hit 키로 사용 — 동일 경로 + 동일
 * size + 동일 mtime 이면 sha256 재계산 skip.
 */
data class FileStat(val sizeBytes: Long, val lastModifiedMs: Long)

expect suspend fun statFile(path: String): FileStat
