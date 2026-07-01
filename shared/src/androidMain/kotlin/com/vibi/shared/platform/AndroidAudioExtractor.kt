package com.vibi.shared.platform

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.vibi.shared.data.local.db.applicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Android 분리 audio 준비 — iOS [IosAudioExtractor] (`AVAssetExportPresetAppleM4A`) 등가.
 *
 * 소스(content:// 영상/오디오)의 오디오 트랙을 (옵션 trim 후) **m4a(AAC)** 로 추출한다.
 * - 트랙이 이미 AAC → `MediaExtractor` + `MediaMuxer` **sample-copy** (재인코딩 0, 무손실, 빠름).
 *   영상 분리(폰 영상 = H.264+AAC) 와 m4a BGM 이 이 경로로 동작 — 본 기능의 주 사용처.
 * - AAC 가 아님(mp3/wav/flac/ogg, 비-AAC 영상) → m4a 컨테이너로 직접 mux 불가라 `MediaCodec`
 *   **디코드→PCM→AAC 재인코딩→mux** 경로로 처리. iOS `AVAssetExportPresetAppleM4A` 가 모든 입력을
 *   transcode 하는 것과 동일하게 디코드 가능한 모든 입력이 m4a(AAC LC, 소스 sampleRate/channels, 192k)로
 *   변환된다.
 *
 * trim 의미는 iOS 와 일치: **startMs/endMs 가 모두 non-null 이고 endMs>startMs 일 때만** 적용하고
 * 트랙 길이로 clamp, 그 외엔 전체 구간(full copy). 두 경로 모두 같은 [startUs]/[endUs] 를 받는다.
 *
 * `isSupported=true` 이므로 commonMain 의 분리 파이프라인(InputViewModel/Repository/BFF/poll)이
 * 그대로 활성화된다. content:// URI 는 [readFileBytes] 와 동일하게 contentResolver 로 연다.
 * 실패는 [AudioExtractException] sealed 변형으로 던져 UI 가 사용자 메시지로 매핑한다.
 */
class AndroidAudioExtractor : AudioExtractor {

    override val isSupported: Boolean = true

    override suspend fun prepareSeparationAudio(
        sourceUri: String,
        sourceKind: AudioSourceKind,
        startMs: Long?,
        endMs: Long?,
    ): PreparedAudio = withContext(Dispatchers.IO) {
        val outDir = File(applicationContext.cacheDir, "separation-prep").apply { mkdirs() }
        val outFile = File(outDir, "${sourceUri.hashCode().toUInt()}_${startMs ?: 0}_${endMs ?: 0}.m4a")
        if (outFile.exists()) outFile.delete()

        val extractor = MediaExtractor()
        try {
            extractor.setPlatformSource(sourceUri)
            val audioTrack = extractor.firstAudioTrackIndex() ?: throw AudioExtractException.SourceCorrupt
            extractor.selectTrack(audioTrack)
            val format = extractor.getTrackFormat(audioTrack)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: throw AudioExtractException.SourceCorrupt

            // iOS 와 동일한 부분 trim 의미: start/end 둘 다 있고 end>start 일 때만 trim, 트랙 길이로 clamp.
            // 한쪽만 있거나 invalid 면 trim 없이 전체 구간 복사/변환.
            val applyTrim = startMs != null && endMs != null && endMs > startMs
            val startUs: Long?
            val endUs: Long?
            if (applyTrim) {
                val durationUs =
                    if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else -1L
                var s = (startMs!! * 1000L).coerceAtLeast(0L)
                var e = endMs!! * 1000L
                if (durationUs > 0L) {
                    s = s.coerceAtMost(durationUs)
                    e = e.coerceAtMost(durationUs)
                }
                startUs = s
                endUs = e
            } else {
                startUs = null
                endUs = null
            }

            if (mime == MediaFormat.MIMETYPE_AUDIO_AAC) {
                sampleCopyToM4a(extractor, format, outFile.absolutePath, startUs, endUs)
            } else {
                // 비-AAC 소스: m4a 에 sample-copy 불가 → 디코드→PCM→AAC 재인코딩→mux.
                transcodeToAacM4a(extractor, format, outFile.absolutePath, startUs, endUs)
            }
            PreparedAudio(path = outFile.absolutePath, mimeType = "audio/mp4", ext = "m4a")
        } catch (e: AudioExtractException) {
            runCatching { outFile.delete() }
            throw e
        } catch (e: Exception) {
            runCatching { outFile.delete() }
            throw mapError(e)
        } finally {
            runCatching { extractor.release() }
        }
    }

    /**
     * 선택된 AAC 오디오 트랙을 새 m4a(MPEG-4) 컨테이너로 sample-copy. 재인코딩 없음.
     * trim: [startUs] 부터 seek, [endUs] 초과 샘플은 중단. 출력 PTS 는 start 기준 0 으로 normalize.
     */
    private fun sampleCopyToM4a(
        extractor: MediaExtractor,
        format: MediaFormat,
        outPath: String,
        startUs: Long?,
        endUs: Long?,
    ) {
        val muxer = MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val maxInput = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else 0
        val buffer = ByteBuffer.allocate(maxInput.coerceAtLeast(256 * 1024))
        val info = MediaCodec.BufferInfo()
        var wroteAny = false
        try {
            val outTrack = muxer.addTrack(format)
            muxer.start()
            if (startUs != null) extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val baseUs = startUs ?: 0L
            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val ptsUs = extractor.sampleTime
                if (endUs != null && ptsUs > endUs) break
                if (ptsUs < baseUs) { extractor.advance(); continue }
                info.offset = 0
                info.size = size
                info.presentationTimeUs = ptsUs - baseUs
                info.flags =
                    if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                        MediaCodec.BUFFER_FLAG_KEY_FRAME
                    } else 0
                muxer.writeSampleData(outTrack, buffer, info)
                wroteAny = true
                if (!extractor.advance()) break
            }
        } finally {
            // wroteAny=false 이면 muxer.stop() 이 IllegalStateException — release 만.
            if (wroteAny) runCatching { muxer.stop() }
            runCatching { muxer.release() }
        }
        if (!wroteAny) throw AudioExtractException.SourceCorrupt
    }

    /**
     * 비-AAC 트랙을 m4a(AAC LC) 로 재인코딩. iOS `AVAssetExportPresetAppleM4A` 의 transcode 등가.
     *
     * 파이프라인: `MediaExtractor` → `MediaCodec` 디코더(→16-bit PCM) → `MediaCodec` AAC 인코더
     * (소스 sampleRate/channels, 192k) → `MediaMuxer`(MPEG-4). trim 은 [startUs] 로 seek 후 디코드된
     * PCM 을 [startUs]/[endUs] 경계로 sample-precise 하게 잘라 인코더에 공급한다(출력 PTS 0 기준 재정렬).
     *
     * 디코더가 float PCM 을 내보내는 기기 대비 [AudioFormat.ENCODING_PCM_16BIT] 요청 + 런타임 변환을
     * 둘 다 둔다(AAC 인코더는 16-bit interleaved PCM 입력).
     */
    private fun transcodeToAacM4a(
        extractor: MediaExtractor,
        inputFormat: MediaFormat,
        outPath: String,
        startUs: Long?,
        endUs: Long?,
    ) {
        val inputMime = inputFormat.getString(MediaFormat.KEY_MIME) ?: throw AudioExtractException.SourceCorrupt
        val sampleRate = inputFormat.optInt(MediaFormat.KEY_SAMPLE_RATE, 44100).coerceAtLeast(1)
        val channelCount = inputFormat.optInt(MediaFormat.KEY_CHANNEL_COUNT, 2).coerceAtLeast(1)
        val bytesPerFrame = 2 * channelCount // 16-bit interleaved PCM

        // 디코더가 16-bit PCM 을 내보내도록 요청(미준수 기기는 아래 toPcm16 가 float→16bit 변환).
        inputFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)

        val decoder = try {
            MediaCodec.createDecoderByType(inputMime).apply {
                configure(inputFormat, null, null, 0)
                start()
            }
        } catch (e: Exception) {
            throw AudioExtractException.CodecUnsupported
        }

        val encoderFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRate,
            channelCount,
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 192_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
        }
        val encoder = try {
            MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }
        } catch (e: Exception) {
            runCatching { decoder.stop() }
            runCatching { decoder.release() }
            throw AudioExtractException.CodecUnsupported
        }

        val muxer = MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerTrack = -1
        var muxerStarted = false
        var wroteAny = false

        // 디코더 출력 PCM(트림 적용) 을 인코더로 흘려보내는 단일 pending 버퍼. pending 이 비어야
        // 다음 디코더 출력을 dequeue (한 번에 한 청크만 in-flight).
        var pending: ByteArray? = null
        var pendingPos = 0
        var pendingEnd = 0
        var pcmIsFloat = false

        // 인코더 입력 PTS 는 공급한 PCM frame 누계로 산출 → 0 기준 monotonic (트림 경계 정렬 무관).
        var encFrames = 0L

        var extractorDone = false
        var decoderDone = false
        var encoderInputDone = false
        var encoderDone = false

        val decInfo = MediaCodec.BufferInfo()
        val encInfo = MediaCodec.BufferInfo()
        val timeoutUs = 10_000L

        try {
            if (startUs != null) extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            while (!encoderDone) {
                // 1) Extractor → 디코더 입력
                if (!extractorDone) {
                    val inIndex = decoder.dequeueInputBuffer(timeoutUs)
                    if (inIndex >= 0) {
                        val inBuf = decoder.getInputBuffer(inIndex)
                        val size = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                        if (size < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            extractorDone = true
                        } else {
                            val sampleTime = extractor.sampleTime
                            if (endUs != null && sampleTime > endUs) {
                                // 트림 끝을 지난 샘플 — 더 디코드할 필요 없음. EOS 로 디코더 flush.
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                extractorDone = true
                            } else {
                                decoder.queueInputBuffer(inIndex, 0, size, sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                // 2) 디코더 출력 → pending PCM (pending 이 비었을 때만)
                if (!decoderDone && pending == null) {
                    val outIndex = decoder.dequeueOutputBuffer(decInfo, timeoutUs)
                    when {
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            pcmIsFloat = decoder.outputFormat
                                .optInt(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT) ==
                                AudioFormat.ENCODING_PCM_FLOAT
                        }

                        outIndex >= 0 -> {
                            val eos = decInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            val isConfig = decInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                            if (decInfo.size > 0 && !isConfig) {
                                val outBuf = decoder.getOutputBuffer(outIndex)
                                if (outBuf != null) {
                                    val pcm16 = toPcm16(outBuf, decInfo.offset, decInfo.size, pcmIsFloat)
                                    val (off, len) = trimRange(
                                        pcm16.size, decInfo.presentationTimeUs, sampleRate, bytesPerFrame, startUs, endUs,
                                    )
                                    if (len > 0) {
                                        pending = pcm16
                                        pendingPos = off
                                        pendingEnd = off + len
                                    }
                                }
                            }
                            decoder.releaseOutputBuffer(outIndex, false)
                            if (eos) decoderDone = true
                        }
                        // INFO_TRY_AGAIN_LATER 등 → 다음 루프
                    }
                }

                // 3) pending PCM (또는 디코더 EOS) → 인코더 입력
                if (!encoderInputDone && (pending != null || decoderDone)) {
                    val inIndex = encoder.dequeueInputBuffer(timeoutUs)
                    if (inIndex >= 0) {
                        val inBuf = encoder.getInputBuffer(inIndex)
                        val data = pending
                        if (data != null && inBuf != null) {
                            inBuf.clear()
                            val cap = inBuf.capacity()
                            var chunk = minOf(pendingEnd - pendingPos, cap)
                            if (chunk >= bytesPerFrame) chunk -= chunk % bytesPerFrame // frame 정렬
                            inBuf.put(data, pendingPos, chunk)
                            val ptsUs = encFrames * 1_000_000L / sampleRate
                            encoder.queueInputBuffer(inIndex, 0, chunk, ptsUs, 0)
                            encFrames += (chunk / bytesPerFrame).toLong()
                            pendingPos += chunk
                            if (pendingPos >= pendingEnd) pending = null
                        } else {
                            // pending 없음 + 디코더 EOS → 인코더 입력 EOS
                            val ptsUs = encFrames * 1_000_000L / sampleRate
                            encoder.queueInputBuffer(inIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            encoderInputDone = true
                        }
                    }
                }

                // 4) 인코더 출력 → muxer
                val outIndex = encoder.dequeueOutputBuffer(encInfo, timeoutUs)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            muxerTrack = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    }

                    outIndex >= 0 -> {
                        val encBuf = encoder.getOutputBuffer(outIndex)
                        if (encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) encInfo.size = 0
                        if (encInfo.size > 0 && muxerStarted && encBuf != null) {
                            encBuf.position(encInfo.offset)
                            encBuf.limit(encInfo.offset + encInfo.size)
                            muxer.writeSampleData(muxerTrack, encBuf, encInfo)
                            wroteAny = true
                        }
                        encoder.releaseOutputBuffer(outIndex, false)
                        if (encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) encoderDone = true
                    }
                    // INFO_TRY_AGAIN_LATER 등 → 다음 루프
                }
            }
        } finally {
            runCatching { decoder.stop() }
            runCatching { decoder.release() }
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            if (wroteAny) runCatching { muxer.stop() }
            runCatching { muxer.release() }
        }
        if (!wroteAny) throw AudioExtractException.SourceCorrupt
    }

    /**
     * 디코더 출력 버퍼 [offset, offset+size) 를 16-bit little-endian interleaved PCM ByteArray 로.
     * 이미 16-bit 면 그대로 복사, float PCM 이면 [-1,1] → 16-bit 변환.
     */
    private fun toPcm16(buf: ByteBuffer, offset: Int, size: Int, isFloat: Boolean): ByteArray {
        if (!isFloat) {
            val arr = ByteArray(size)
            buf.position(offset)
            buf.limit(offset + size)
            buf.get(arr, 0, size)
            return arr
        }
        buf.position(offset)
        buf.limit(offset + size)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        val fb = buf.asFloatBuffer()
        val n = fb.remaining()
        val out = ByteArray(n * 2)
        val ob = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)
        var i = 0
        while (i < n) {
            val s = (fb.get(i) * 32767f).coerceIn(-32768f, 32767f).toInt()
            ob.putShort(s.toShort())
            i++
        }
        return out
    }

    /**
     * PCM 버퍼(presentationTime [bufPtsUs]) 중 [startUs]/[endUs] 윈도와 겹치는 frame 정렬 sub-range
     * (offset, length) 산출. 경계 밖이면 length 0.
     */
    private fun trimRange(
        pcmSize: Int,
        bufPtsUs: Long,
        sampleRate: Int,
        bytesPerFrame: Int,
        startUs: Long?,
        endUs: Long?,
    ): Pair<Int, Int> {
        if (bytesPerFrame <= 0 || pcmSize <= 0) return 0 to 0
        var off = 0
        var len = pcmSize
        if (startUs != null && bufPtsUs < startUs) {
            val dropUs = startUs - bufPtsUs
            val dropFrames = dropUs * sampleRate / 1_000_000L
            val dropBytes = (dropFrames * bytesPerFrame).coerceIn(0L, pcmSize.toLong()).toInt()
            off = dropBytes
            len = pcmSize - dropBytes
        }
        if (endUs != null) {
            val keepUs = endUs - bufPtsUs
            if (keepUs <= 0L) return off to 0
            val keepFrames = keepUs * sampleRate / 1_000_000L
            val keepBytes = (keepFrames * bytesPerFrame).coerceIn(0L, pcmSize.toLong()).toInt()
            len = (keepBytes - off).coerceIn(0, len)
        }
        return off to len
    }

    private fun mapError(e: Exception): AudioExtractException = when {
        e is IOException && (
            e.message?.contains("ENOSPC", ignoreCase = true) == true ||
                e.message?.contains("No space", ignoreCase = true) == true
            ) -> AudioExtractException.DiskFull
        e is FileNotFoundException -> AudioExtractException.SourceCorrupt
        else -> AudioExtractException.Unknown(e.message ?: e::class.simpleName ?: "unknown")
    }
}
