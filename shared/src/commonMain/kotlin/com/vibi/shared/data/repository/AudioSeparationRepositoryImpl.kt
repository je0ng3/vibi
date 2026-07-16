package com.vibi.shared.data.repository

import com.vibi.shared.data.local.CreditStore
import com.vibi.shared.data.local.UserSession
import com.vibi.shared.data.remote.api.BffApi
import com.vibi.shared.data.remote.api.BinaryPart
import com.vibi.shared.data.remote.dto.BffErrorResponse
import com.vibi.shared.data.remote.dto.SeparationSpec
import com.vibi.shared.domain.error.InsufficientCreditsException
import com.vibi.shared.domain.model.SeparationCost
import com.vibi.shared.domain.model.Stem
import com.vibi.shared.domain.util.absoluteMediaUrl
import com.vibi.shared.domain.repository.AudioSeparationRepository
import com.vibi.shared.domain.repository.SeparationStatus
import com.vibi.shared.platform.AudioExtractException
import com.vibi.shared.platform.AudioExtractor
import com.vibi.shared.platform.AudioSourceKind
import com.vibi.shared.platform.persistentStemsDirPath
import com.vibi.shared.platform.readFileBytes
import com.vibi.shared.platform.writeChannelToFile
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import kotlin.coroutines.cancellation.CancellationException
import io.ktor.http.HttpStatusCode

class AudioSeparationRepositoryImpl(
    private val api: BffApi,
    private val bffBaseUrl: String,
    /** trim + audio extract → m4a 생성. 안드로이드 stub 분기 차단도 본 객체의 `isSupported` 로
     *  결정 — Android actual 은 `false` 라 진입 전 typed exception 으로 떨어짐. */
    private val audioExtractor: AudioExtractor,
    /** 402 응답의 권위 잔액으로 로컬 캐시 동기화. null 이면 동기화 skip (테스트). */
    private val creditStore: CreditStore? = null,
    /** 현재 로그인 사용자 식별 — credit cache key. null 이면 동기화 skip. */
    private val userSession: UserSession? = null,
) : AudioSeparationRepository {

    /** stem URL 이 path-only (`/api/v2/...`) 면 BFF base 와 join 해 absolute URL 로. */
    private fun absUrl(pathOrUrl: String): String = absoluteMediaUrl(bffBaseUrl, pathOrUrl)

    override suspend fun getCost(durationMs: Long): Result<SeparationCost> = runCatching {
        val resp = api.getCreditCost(durationMs)
        // BFF 권위 잔액으로 로컬 캐시 동기화 — UserMenu 같은 다른 화면이 즉시 최신 값 표시.
        if (creditStore != null && userSession != null) {
            creditStore.setBalance(userSession.current(), resp.balance)
        }
        SeparationCost(
            durationMs = resp.durationMs,
            credits = resp.credits,
            balance = resp.balance,
            sufficient = resp.sufficient,
        )
    }

    override suspend fun startSeparation(
        sourceUri: String,
        sourceKind: AudioSourceKind,
        sourceLanguageCode: String,
        trimStartMs: Long?,
        trimEndMs: Long?,
    ): Result<String> = runCatching {
        // Repository 단 최종 가드 — UI hide / ViewModel 가드 누락 시에도 안드로이드 stub 호출
        // 막아 crash 차단. typed exception 이라 UI 가 사용자 메시지로 graceful 매핑.
        if (!audioExtractor.isSupported) {
            throw AudioExtractException.Unknown("separation not supported on this platform")
        }

        val prepared = audioExtractor.prepareSeparationAudio(
            sourceUri = sourceUri,
            sourceKind = sourceKind,
            startMs = trimStartMs,
            endMs = trimEndMs,
        )
        val bytes = readFileBytes(prepared.path)
        val part = BinaryPart(
            fieldName = "file",
            filename = "separation.${prepared.ext}",
            bytes = bytes,
            contentType = prepared.mimeType,
        )
        val spec = SeparationSpec(sourceLanguageCode = sourceLanguageCode)
        val jobId = try {
            api.startSeparation(file = part, spec = spec).jobId
        } catch (e: ClientRequestException) {
            throw mapInsufficientCreditsOrRethrow(e)
        }
        // 분리 시작 = 크레딧 차감 시점(402 도 이 시점에 발생). 성공 직후 권위 잔액으로 로컬 캐시를
        // 동기화한다 — 이게 없으면 홈/프로필의 크레딧 배지가 프로필 재진입(UserMenuVM.refreshBalance)
        // 전까지 차감 전 값으로 stale 하게 남는다. getCost(차감 전)·402(실패) 동기화와 짝을 이루는
        // 성공 경로 동기화. 모든 분리 시작(전체영상/구간)이 본 메서드를 거치므로 한 곳이면 충분.
        syncBalanceFromServer()
        jobId
    }

    /**
     * BFF 권위 잔액을 1회 GET 해 로컬 캐시([CreditStore]) 와 동기화. best-effort —
     * 실패해도 호출 경로(분리 시작)는 막지 않는다. creditStore/userSession 미주입(테스트) 이면 no-op.
     */
    private suspend fun syncBalanceFromServer() {
        val store = creditStore ?: return
        val session = userSession ?: return
        runCatching { api.getCreditBalance() }
            .onSuccess { store.setBalance(session.current(), it.balance) }
    }

    /**
     * 402 응답 파싱. body 가 `BffErrorResponse(error="insufficient_credits", detail="required=N balance=M")`
     * 형식이라 detail 을 정규식으로 분해. detail 파싱 실패 시 fallback (required=0, balance=0) 으로
     * exception 던지되 UI 는 "충전 필요" 분기로 유도. 다른 4xx 는 원본 그대로 rethrow.
     *
     * Side effect: balance 파싱에 성공하면 [CreditStore] 도 갱신 — 사용자가 다른 화면 (UserMenu)
     * 으로 이동해 충전 시작 직전 잔액이 이미 최신.
     */
    private suspend fun mapInsufficientCreditsOrRethrow(e: ClientRequestException): Throwable {
        if (e.response.status.value != HTTP_PAYMENT_REQUIRED) return e
        val body = runCatching { e.response.body<BffErrorResponse>() }.getOrNull()
        if (body?.error != ERROR_INSUFFICIENT_CREDITS) return e
        val (required, balance) = parseRequiredBalance(body.detail)
        if (creditStore != null && userSession != null) {
            creditStore.setBalance(userSession.current(), balance)
        }
        return InsufficientCreditsException(required = required, balance = balance)
    }

    private fun parseRequiredBalance(detail: String?): Pair<Int, Int> {
        if (detail == null) return 0 to 0
        // "required=2 balance=1" 형식. BFF SeparationRoutes.kt 의 ApiErrorException detail 생성과 1:1.
        // 형식 변경 시 양쪽 동시 갱신.
        val req = REQUIRED_REGEX.find(detail)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val bal = BALANCE_REGEX.find(detail)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        return req to bal
    }

    override suspend fun pollStatus(jobId: String): Result<SeparationStatus> = try {
        val response = api.getSeparationStatus(jobId)
        val status = when {
            response.status == STATUS_FAILED -> {
                // 분리 실패 = BFF 가 선차감 크레딧을 환불(onJobFailed 훅)하는 시점. 권위 잔액을 재동기화해
                // 사용자가 홈/타임라인을 떠나지 않아도 환불된 배지가 바로 보이게 한다 — getCost(차감 전)·
                // 성공·402 동기화와 짝을 이루는 실패 경로 동기화. best-effort 라 실패해도 폴링 결과는 그대로.
                syncBalanceFromServer()
                // raw progressReason("Failed")이 아니라 BFF 가 sanitize 해 내려준 friendly error 를 표시.
                // (문구 단일 소스=BFF — 클라 코드→문구 재매핑 없음.)
                SeparationStatus.Failed(
                    jobId = response.jobId,
                    message = response.error,
                    errorCode = response.errorCode,
                )
            }

            response.status == STATUS_READY && response.stems.isNotEmpty() ->
                SeparationStatus.Ready(
                    jobId = response.jobId,
                    stems = response.stems.map {
                        Stem(
                            stemId = it.stemId,
                            label = it.label,
                            url = absUrl(it.url),
                            kind = Stem.kindFromId(it.stemId),
                            speakerIndex = Stem.speakerIndexFromId(it.stemId)
                        )
                    },
                    actualDurationMs = response.actualDurationMs
                )

            else -> SeparationStatus.Processing(
                jobId = response.jobId,
                progress = response.progress,
                progressReason = response.progressReason
            )
        }
        Result.success(status)
    } catch (e: CancellationException) {
        // 사용자 취소 — 동기화/래핑 없이 그대로 전파(구조적 동시성 보존). runCatching 의 취소 삼킴 회피.
        throw e
    } catch (e: Throwable) {
        // 폴링 조회 자체 실패(네트워크/5xx 등) — 잡이 서버에서 실패·환불됐을 수 있어 잔액 재동기화.
        // getCreditBalance 도 같은 네트워크면 best-effort no-op. 폴링 실패는 그대로 호출자에 전파.
        syncBalanceFromServer()
        Result.failure(e)
    }

    override suspend fun downloadStem(stemUrl: String, outputFileName: String): Result<String> =
        runCatching {
            // 영구 디렉터리에 스트리밍 저장 — 전체 적재 없이 청크 기록. 서버가 끊겨도 오프라인
            // 재생/편집이 가능하도록 evict 안 되는 stem 디렉터리에 둔다.
            val destPath = "${persistentStemsDirPath()}/$outputFileName"
            api.downloadStem(stemUrl) { writeChannelToFile(it, destPath) }
            destPath
        }

    private companion object {
        const val STATUS_READY = "READY"
        const val STATUS_FAILED = "FAILED"
        const val HTTP_PAYMENT_REQUIRED = 402
        const val ERROR_INSUFFICIENT_CREDITS = "insufficient_credits"
        val REQUIRED_REGEX = Regex("required=(\\d+)")
        val BALANCE_REGEX = Regex("balance=(\\d+)")
    }
}
