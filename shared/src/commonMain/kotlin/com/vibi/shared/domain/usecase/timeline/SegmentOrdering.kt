package com.vibi.shared.domain.usecase.timeline

import com.vibi.shared.domain.model.Segment
import com.vibi.shared.domain.repository.SegmentRepository

/**
 * 이미 order 로 정렬된 [sortedSegments] 의 order 를 0..n-1 로 연속 재배치 후 persist.
 * 변경된 row 만 update 해 불필요한 Room emit 을 줄인다. (호출자가 정렬·필터를 책임지고
 * 메모리 리스트를 넘긴다 — 재조회하지 않음.)
 */
suspend fun SegmentRepository.compactOrders(sortedSegments: List<Segment>) {
    sortedSegments.forEachIndexed { index, seg ->
        if (seg.order != index) updateSegment(seg.copy(order = index))
    }
}

/**
 * [projectId] 에서 order 가 [afterOrder] 보다 큰 세그먼트들의 order 를 [by] 만큼 뒤로 밀어
 * 삽입 공간을 확보. 높은 order 부터 갱신해 (unique order index 가 있을 경우의) 일시적 충돌을 피한다.
 */
suspend fun SegmentRepository.shiftOrdersAfter(projectId: String, afterOrder: Int, by: Int) {
    getByProjectId(projectId)
        .filter { it.order > afterOrder }
        .sortedByDescending { it.order }
        .forEach { updateSegment(it.copy(order = it.order + by)) }
}
