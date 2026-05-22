package com.vibi.shared.domain.usecase.separation

import kotlinx.coroutines.delay

sealed class PollDecision<out R> {
    data class Ready<R>(val value: R) : PollDecision<R>()
    data class Failed(val reason: String?) : PollDecision<Nothing>()
    data object Processing : PollDecision<Nothing>()
}

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
