package com.vibi.shared.domain.usecase.separation

import com.vibi.shared.domain.repository.AudioSeparationRepository
import com.vibi.shared.domain.repository.SeparationStatus
import com.vibi.shared.domain.usecase.subtitle.PollDecision
import com.vibi.shared.domain.usecase.subtitle.pollUntilReady
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PollSeparationUseCase constructor(
    private val repository: AudioSeparationRepository,
) {
    /**
     * 30분 timeout 까지 [intervalMs] 마다 status 조회. Processing 도 emit 하므로 호출자는 Flow.collect.
     * Ready/Failed/Consumed terminal 상태 emit 후 flow 종료. timeout 시 IllegalStateException.
     *
     * intervalMs = 10s × maxAttempts = 180 → ~30분.
     */
    operator fun invoke(jobId: String, intervalMs: Long = 10_000L): Flow<SeparationStatus> = flow {
        val terminal: SeparationStatus = pollUntilReady(
            label = "separation",
            pollIntervalMs = intervalMs,
            maxAttempts = MAX_ATTEMPTS,
            fetch = { repository.pollStatus(jobId).getOrThrow() },
            onProcessing = { status -> emit(status) },
        ) { status ->
            when (status) {
                is SeparationStatus.Ready,
                is SeparationStatus.Consumed,
                is SeparationStatus.Failed -> PollDecision.Ready(status)
                is SeparationStatus.Processing -> PollDecision.Processing
            }
        }
        emit(terminal)
    }

    private companion object {
        // 10s × 180 = 30분.
        const val MAX_ATTEMPTS = 180
    }
}
