package com.vibi.shared.domain.usecase.lipsync

import com.vibi.shared.domain.repository.LipSyncRepository
import com.vibi.shared.domain.repository.LipSyncStatus
import com.vibi.shared.domain.usecase.subtitle.PollDecision
import com.vibi.shared.domain.usecase.subtitle.pollUntilReady
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PollLipSyncUseCase constructor(
    private val lipSyncRepository: LipSyncRepository,
) {
    /**
     * 20분 timeout 까지 [intervalMs] 마다 status 조회. completed 후 flow 종료. timeout 시 IllegalStateException.
     *
     * intervalMs = 5s × maxAttempts = 240 → ~20분.
     */
    operator fun invoke(jobId: String, intervalMs: Long = 5_000L): Flow<LipSyncStatus> = flow {
        val terminal = pollUntilReady(
            label = "lipsync",
            pollIntervalMs = intervalMs,
            maxAttempts = MAX_ATTEMPTS,
            fetch = { lipSyncRepository.pollStatus(jobId).getOrThrow() },
            onProcessing = { status -> emit(status) },
        ) { status ->
            if (status.isCompleted) PollDecision.Ready(status) else PollDecision.Processing
        }
        emit(terminal)
    }

    private companion object {
        // 5s × 240 = 20분.
        const val MAX_ATTEMPTS = 240
    }
}
