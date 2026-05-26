package com.vibi.shared.platform

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * NIST FIPS 180-4 / common test vectors. Sha256 알고리즘 회귀 가드.
 */
class Sha256Test {
    @Test
    fun emptyInput() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Sha256().digestHex(),
        )
    }

    @Test
    fun abc() {
        val h = Sha256()
        h.update("abc".encodeToByteArray())
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            h.digestHex(),
        )
    }

    @Test
    fun fiftyFiveBytes() {
        val h = Sha256()
        h.update("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray())
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            h.digestHex(),
        )
    }

    @Test
    fun chunkedUpdateMatchesSingleShot() {
        // 200 bytes = 3+ full blocks + tail. update path 가 spanning block boundary 에서도
        // single-shot 과 동일 결과 내는지 — algorithm 정확성 + buffer 매니지먼트 회귀 가드.
        val data = ByteArray(200) { (it * 7).toByte() }
        val single = Sha256().also { it.update(data) }.digestHex()
        val chunked = Sha256().also {
            it.update(data, 0, 64)
            it.update(data, 64, 73)
            it.update(data, 137, data.size - 137)
        }.digestHex()
        assertEquals(single, chunked)
    }
}
