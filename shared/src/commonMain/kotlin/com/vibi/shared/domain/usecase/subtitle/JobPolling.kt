package com.vibi.shared.domain.usecase.subtitle

import kotlinx.coroutines.delay

/**
 * Polling 분류 결과 — fetch 가 반환한 status 를 ready/failed/processing 으로 매핑.
 */
sealed class PollDecision<out R> {
    data class Ready<R>(val value: R) : PollDecision<R>()
    data class Failed(val reason: String?) : PollDecision<Nothing>()
    data object Processing : PollDecision<Nothing>()
}

/**
 * 공용 polling helper — subtitle / autodub / separation / lipsync 모두 사용.
 *
 * fetch 를 [maxAttempts] 회 호출하며, classify 가 Ready 면 값을 반환, Failed 면 throw,
 * Processing 이면 [pollIntervalMs] 대기 후 재시도. maxAttempts 도달 시 timeout throw.
 *
 * @param onProcessing 매 attempt 의 fetch 결과를 사용자에게 노출 (e.g. progress emit). 기본 no-op.
 */
suspend fun <T, R> pollUntilReady(
    label: String,
    pollIntervalMs: Long,
    maxAttempts: Int,
    fetch: suspend () -> T,
    onProcessing: suspend (T) -> Unit = {},
    classify: (T) -> PollDecision<R>,
): R {
    repeat(maxAttempts) {
        val current = fetch()
        when (val decision = classify(current)) {
            is PollDecision.Ready -> return decision.value
            is PollDecision.Failed ->
                throw IllegalStateException(decision.reason ?: "$label job failed")
            PollDecision.Processing -> {
                onProcessing(current)
                delay(pollIntervalMs)
            }
        }
    }
    throw IllegalStateException(
        "$label job timed out after ${maxAttempts * pollIntervalMs / 1000}s"
    )
}
