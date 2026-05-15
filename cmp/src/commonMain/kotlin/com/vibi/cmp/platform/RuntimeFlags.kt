package com.vibi.cmp.platform

/**
 * 런타임 토글 — 디자인 변경처럼 점진 이행이 필요한 기능을 코드에 둔 채로 끄고 켤 수 있게.
 * BuildConfig / xcconfig 와 달리 컴파일 없이 한 곳만 바꾸면 됨.
 *
 * 본 객체 자체가 SSOT — 향후 디버그 메뉴(긴 탭 등)에서 동적 변경하려면 mutable 로 바꾸고
 * remember/snapshotFlow 로 관찰. 지금은 컴파일타임 상수만 필요.
 */
object RuntimeFlags {
    /** Timeline 의 SoundDeck (카드 스택) 노출. 기존 AudioSeparationSheet 과 병행. */
    const val soundDeckEnabled: Boolean = true

    /**
     * Timeline 의 3단계 stepper UI 와 단계별 분기 숨김 — AudioSources + SubtitleDub 을
     * 한 스크롤로 노출. Edit 단계 자동 진입도 비활성. true 가 새 디자인 기본.
     */
    const val stepperHidden: Boolean = true
}
