---
version: alpha
name: vibi (ElevenLabs-inspired, mobile-tuned)
description: vibi 모바일 클라이언트(KMP/CMP, iOS-first) 디자인 시스템. ElevenLabs 의 편집(editorial)·매거진 무드를 iOS/Android 폼팩터에 맞춰 재튜닝. 베이스 캔버스는 off-white(`#f5f5f5`)에 따뜻한 near-black ink(`#0c0a09`); 브랜드 보이지는 채도가 아닌 **사진/대기(atmosphere)** 로 — 파스텔 그라디언트 orb(mint/peach/lavender/sky/rose)가 유일한 "컬러" 모먼트. Display 는 EB Garamond Light(weight 300, Waldenburg 대체), Body 는 Inter. CTA 는 잉크 핀(near-black pill) 단일. 데스크톱 64px hero → 모바일 40px 로 캡, 96px section rhythm → 48px(모바일)/64px(태블릿)로 압축, pill 높이 40→48px 로 iOS 44pt HIG 준수.

colors:
  primary: "#292524"
  primary-active: "#0c0a09"
  ink: "#0c0a09"
  body: "#4e4e4e"
  body-strong: "#292524"
  muted: "#777169"
  muted-soft: "#a8a29e"
  hairline: "#e7e5e4"
  hairline-soft: "#f0efed"
  hairline-strong: "#d6d3d1"
  canvas: "#f5f5f5"
  canvas-soft: "#fafafa"
  canvas-deep: "#0c0a09"
  surface-card: "#ffffff"
  surface-strong: "#f0efed"
  surface-dark: "#0c0a09"
  surface-dark-elevated: "#1c1917"
  on-primary: "#ffffff"
  on-dark: "#ffffff"
  on-dark-soft: "#a8a29e"
  gradient-mint: "#a7e5d3"
  gradient-peach: "#f4c5a8"
  gradient-lavender: "#c8b8e0"
  gradient-sky: "#a8c8e8"
  gradient-rose: "#e8b8c4"
  semantic-error: "#dc2626"
  semantic-success: "#16a34a"
  subtitle-overlay-bg: "#cc0c0a09"
  timeline-bar-track: "#e7e5e4"
  timeline-bar-segment: "#a8a29e"
  timeline-bar-segment-edited: "#c8b8e0"
  timeline-bar-directive: "#f4c5a8"
  dark-canvas: "#0c0a09"
  dark-surface-card: "#1c1917"
  dark-surface-strong: "#292524"
  dark-hairline: "#292524"
  dark-ink: "#fafafa"
  dark-body: "#a8a29e"

typography:
  display-hero:
    fontFamily: "'EB Garamond', 'Times New Roman', serif"
    fontSize: 40px
    fontWeight: 300
    lineHeight: 1.08
    letterSpacing: -0.8px
  display-xl:
    fontFamily: "'EB Garamond', serif"
    fontSize: 32px
    fontWeight: 300
    lineHeight: 1.13
    letterSpacing: -0.32px
  display-lg:
    fontFamily: "'EB Garamond', serif"
    fontSize: 28px
    fontWeight: 300
    lineHeight: 1.18
    letterSpacing: -0.28px
  display-md:
    fontFamily: "'EB Garamond', serif"
    fontSize: 24px
    fontWeight: 300
    lineHeight: 1.2
    letterSpacing: -0.24px
  display-sm:
    fontFamily: "'EB Garamond', serif"
    fontSize: 20px
    fontWeight: 300
    lineHeight: 1.25
    letterSpacing: 0
  title-lg:
    fontFamily: "'Inter', sans-serif"
    fontSize: 18px
    fontWeight: 500
    lineHeight: 1.4
    letterSpacing: 0
  title-md:
    fontFamily: "'Inter', sans-serif"
    fontSize: 17px
    fontWeight: 500
    lineHeight: 1.4
    letterSpacing: 0
  title-sm:
    fontFamily: "'Inter', sans-serif"
    fontSize: 15px
    fontWeight: 500
    lineHeight: 1.4
    letterSpacing: 0.15px
  body-md:
    fontFamily: "'Inter', sans-serif"
    fontSize: 16px
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: 0.16px
  body-strong:
    fontFamily: "'Inter', sans-serif"
    fontSize: 16px
    fontWeight: 500
    lineHeight: 1.5
    letterSpacing: 0.16px
  body-sm:
    fontFamily: "'Inter', sans-serif"
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: 0.14px
  caption:
    fontFamily: "'Inter', sans-serif"
    fontSize: 13px
    fontWeight: 400
    lineHeight: 1.4
    letterSpacing: 0
  caption-uppercase:
    fontFamily: "'Inter', sans-serif"
    fontSize: 11px
    fontWeight: 600
    lineHeight: 1.4
    letterSpacing: 0.88px
    textTransform: uppercase
  button:
    fontFamily: "'Inter', sans-serif"
    fontSize: 16px
    fontWeight: 500
    lineHeight: 1.0
    letterSpacing: 0
  tab-label:
    fontFamily: "'Inter', sans-serif"
    fontSize: 11px
    fontWeight: 500
    lineHeight: 1.2
    letterSpacing: 0.11px
  mono-time:
    fontFamily: "'JetBrains Mono', 'SF Mono', monospace"
    fontSize: 13px
    fontWeight: 500
    lineHeight: 1.0
    letterSpacing: 0

rounded:
  none: 0px
  xs: 4px
  sm: 6px
  md: 10px
  lg: 14px
  xl: 18px
  xxl: 24px
  pill: 9999px
  full: 9999px

spacing:
  xxs: 4px
  xs: 8px
  sm: 12px
  base: 16px
  md: 20px
  lg: 24px
  xl: 32px
  xxl: 40px
  section-mobile: 48px
  section-tablet: 64px

components:
  top-bar:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    typography: "{typography.title-md}"
    height: 56px
  tab-bar:
    backgroundColor: "{colors.canvas-soft}"
    textColor: "{colors.muted}"
    activeTextColor: "{colors.ink}"
    typography: "{typography.tab-label}"
    height: 64px
    borderTop: "1px {colors.hairline}"
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.button}"
    rounded: "{rounded.pill}"
    padding: 14px 24px
    height: 48px
    minWidth: 88px
  button-primary-pressed:
    backgroundColor: "{colors.primary-active}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.pill}"
  button-outline:
    backgroundColor: transparent
    textColor: "{colors.ink}"
    typography: "{typography.button}"
    rounded: "{rounded.pill}"
    padding: 13px 23px
    height: 48px
    border: "1px {colors.hairline-strong}"
  button-text:
    backgroundColor: transparent
    textColor: "{colors.ink}"
    typography: "{typography.button}"
    padding: 12px 16px
    minHeight: 44px
  icon-button:
    backgroundColor: transparent
    textColor: "{colors.ink}"
    rounded: "{rounded.full}"
    size: 44px
  icon-button-filled:
    backgroundColor: "{colors.surface-strong}"
    textColor: "{colors.ink}"
    rounded: "{rounded.full}"
    size: 44px
  hero-section:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    typography: "{typography.display-hero}"
    padding: 32px 20px
  feature-card:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.ink}"
    typography: "{typography.title-md}"
    rounded: "{rounded.xl}"
    padding: 20px
    border: "1px {colors.hairline}"
  voice-row:
    backgroundColor: transparent
    textColor: "{colors.ink}"
    typography: "{typography.body-md}"
    padding: 12px 16px
    borderBottom: "1px {colors.hairline-soft}"
    minHeight: 64px
  voice-icon-circular:
    backgroundColor: "{colors.surface-strong}"
    rounded: "{rounded.full}"
    size: 40px
  audio-waveform-card:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.ink}"
    rounded: "{rounded.xl}"
    padding: 16px
  text-input:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.ink}"
    typography: "{typography.body-md}"
    rounded: "{rounded.md}"
    padding: 14px 16px
    height: 52px
    border: "1px {colors.hairline-strong}"
  text-input-focused:
    border: "2px {colors.ink}"
  badge-pill:
    backgroundColor: "{colors.surface-strong}"
    textColor: "{colors.ink}"
    typography: "{typography.caption-uppercase}"
    rounded: "{rounded.pill}"
    padding: 4px 10px
  chip:
    backgroundColor: "{colors.surface-strong}"
    textColor: "{colors.ink}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.pill}"
    padding: 8px 14px
    minHeight: 36px
  chip-disabled:
    backgroundColor: "{colors.hairline-soft}"
    textColor: "{colors.muted-soft}"
    rounded: "{rounded.pill}"
  bottom-sheet:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.ink}"
    rounded: "{rounded.xxl} {rounded.xxl} 0 0"
    padding: 20px
    handleColor: "{colors.hairline-strong}"
    handleSize: 36x4
  panel-card:
    backgroundColor: "{colors.canvas-soft}"
    textColor: "{colors.ink}"
    rounded: "{rounded.lg}"
    padding: 16px
    border: "1px {colors.hairline}"
  subtitle-overlay:
    backgroundColor: "{colors.subtitle-overlay-bg}"
    textColor: "{colors.on-dark}"
    typography: "{typography.body-md}"
    rounded: "{rounded.sm}"
    padding: 6px 10px
  timeline-bar-track:
    backgroundColor: "{colors.timeline-bar-track}"
    height: 4px
    rounded: "{rounded.pill}"
  timeline-bar-segment:
    backgroundColor: "{colors.timeline-bar-segment}"
    rounded: "{rounded.xs}"
  timeline-bar-segment-edited:
    backgroundColor: "{colors.timeline-bar-segment-edited}"
    rounded: "{rounded.xs}"
  timeline-bar-directive:
    backgroundColor: "{colors.timeline-bar-directive}"
    rounded: "{rounded.xs}"
  gradient-orb-card:
    backgroundColor: "{colors.canvas-soft}"
    textColor: "{colors.ink}"
    rounded: "{rounded.xxl}"
    padding: 24px
  cta-section:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    typography: "{typography.display-md}"
    padding: 48px 20px
  list-divider:
    backgroundColor: "{colors.hairline-soft}"
    height: 1px
  snackbar:
    backgroundColor: "{colors.surface-dark}"
    textColor: "{colors.on-dark}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.md}"
    padding: 14px 16px
---

## Overview

vibi 모바일은 ElevenLabs 의 **편집·매거진** 무드를 iOS/Android 단일 폼팩터로 가져온 음성 AI 클라이언트다. 베이스 캔버스 `{colors.canvas}` (#f5f5f5) 위 따뜻한 near-black 잉크 `{colors.ink}` (#0c0a09); 브랜드 voltage 는 채도가 아닌 **사진/대기 그라디언트** 에서 온다. 파스텔 orb 5개(mint/peach/lavender/sky/rose) 가 유일한 "색" 모먼트.

Display 는 **EB Garamond Light** (weight 300, Waldenburg 대체) 으로 잡고 Body 는 **Inter** 가 운반. 이 weight 300 의 가는 세리프가 편집 시그니처. 모바일에선 가독성을 위해 **display-hero 를 40px 까지만 키우고**, body 는 16px (iOS HIG 17pt 와 1pt 차) 로 통일.

CTA 는 잉크 핀 단일 — 채도 있는 브랜드 액션 컬러 없음. 모바일에서 **pill 높이를 48px 로 늘려 iOS 44pt 최소 터치 타깃** 을 안전하게 넘긴다.

이 문서는 데스크톱 `vibi-landing/DESIGN.md` 의 모바일 적응판이며, **`:cmp` (Compose Multiplatform UI 모듈)** 의 디자인 토큰 기준이다.

**핵심 특성:**
- Off-white 캔버스, warm near-black 잉크. 채도 있는 브랜드 액션 컬러 없음.
- 단일 primary action: 잉크 pill `{rounded.pill}`, 높이 48px.
- Display 는 EB Garamond Light(300). 모바일 hero 40px 캡.
- Body 는 Inter 16/+0.16px tracking.
- 파스텔 그라디언트 orb 5종은 **장식용 atmosphere only** — 버튼 fill, 텍스트 컬러 금지.
- Pill geometry (`{rounded.pill}` CTA, `{rounded.xl}` card, `{rounded.xxl}` bottom-sheet top corners).
- 48px section rhythm (모바일), 64px (태블릿).
- vibi 고유 도메인 토큰: `subtitle-overlay-bg`, `timeline-bar-*` (track/segment/edited/directive) 4종 보존.

## Colors

### Brand & Accent
- **Ink Primary** (`{colors.primary}` — #292524): 유일한 primary CTA 색. 인색하게 사용.
- **Ink Primary Active** (`{colors.primary-active}` — #0c0a09): Press 상태.

### Surface
- **Canvas** (`{colors.canvas}` — #f5f5f5): 페이지 바닥(Off-white).
- **Canvas Soft** (`{colors.canvas-soft}` — #fafafa): 살짝 밝은 교차 밴드 / panel-card 바탕.
- **Canvas Deep** (`{colors.canvas-deep}` — #0c0a09): 잉크와 동일 — dark hero / 다크 모드 캔버스.
- **Surface Card** (`{colors.surface-card}` — #ffffff): 순백 카드.
- **Surface Strong** (`{colors.surface-strong}` — #f0efed): chip / badge / voice-icon plate.
- **Surface Dark** (`{colors.surface-dark}` — #0c0a09): snackbar / featured pricing / dark CTA 밴드.
- **Surface Dark Elevated** (`{colors.surface-dark-elevated}` — #1c1917): dark 모드 카드.

### Hairlines
- **Hairline** (`{colors.hairline}` — #e7e5e4): 기본 1px 디바이더.
- **Hairline Soft** (`{colors.hairline-soft}` — #f0efed): 더 흐린 디바이더 (list row).
- **Hairline Strong** (`{colors.hairline-strong}` — #d6d3d1): outline 버튼 / input 보더.

### Text
- **Ink** (`{colors.ink}` — #0c0a09): Display, primary text.
- **Body** (`{colors.body}` — #4e4e4e): 본문.
- **Body Strong** (`{colors.body-strong}` — #292524): 강조.
- **Muted** (`{colors.muted}` — #777169): 서브타이틀.
- **Muted Soft** (`{colors.muted-soft}` — #a8a29e): disabled.
- **On Primary / On Dark** (#ffffff).
- **On Dark Soft** (#a8a29e).

### Atmospheric Gradient Stops (signature)
- **Mint** `#a7e5d3` · **Peach** `#f4c5a8` · **Lavender** `#c8b8e0` · **Sky** `#a8c8e8` · **Rose** `#e8b8c4`.

오직 `{component.gradient-orb-card}` 와 hero 배경 atmosphere 로만. 버튼 fill / 텍스트 컬러로 사용 금지. 모바일에선 GPU 부하 고려해 **한 화면당 orb 1개** 가 기본.

### vibi 도메인 토큰 (preserve)
데스크톱 ElevenLabs 시스템에 없는 모바일 앱 전용 토큰 — 자막 / 타임라인 에디터 / chip 시스템 용:

- **Subtitle Overlay BG** (`{colors.subtitle-overlay-bg}` — #cc0c0a09): 비디오 위 자막 박스 배경 (잉크 80% alpha).
- **Timeline Bar Track** (`{colors.timeline-bar-track}` — #e7e5e4): 통합 타임라인 바 트랙. Hairline 과 동일.
- **Timeline Bar Segment** (`{colors.timeline-bar-segment}` — #a8a29e): 기본 segment. Muted-soft 와 동일 — 중성.
- **Timeline Bar Segment Edited** (`{colors.timeline-bar-segment-edited}` — #c8b8e0): 편집 적용 segment. Lavender orb 톤 — atmosphere 팔레트에서 차용해 일관성 유지.
- **Timeline Bar Directive** (`{colors.timeline-bar-directive}` — #f4c5a8): 음성분리 directive. Peach orb 톤 — edited 와 hue 분리 (lavender vs peach).

### Dark 모드 매핑 (system dark / iOS 사용자 다수)
ElevenLabs 자체는 light-first 지만 iOS 사용자 다크 선호도 고려해 매핑 제공. 다크는 **편집 무드 약화 + 컨트라스트 우선**.

- **Dark Canvas** (`{colors.dark-canvas}` — #0c0a09): 다크 페이지 바닥.
- **Dark Surface Card** (`{colors.dark-surface-card}` — #1c1917): 다크 카드.
- **Dark Surface Strong** (`{colors.dark-surface-strong}` — #292524): 다크 chip / badge.
- **Dark Hairline** (`{colors.dark-hairline}` — #292524): 다크 디바이더.
- **Dark Ink** (`{colors.dark-ink}` — #fafafa): 다크 primary text.
- **Dark Body** (`{colors.dark-body}` — #a8a29e): 다크 body text.

다크 모드에선 **gradient orb 의 alpha 를 60% → 30%** 로 낮춰 어두운 캔버스 위에서 둥둥 뜨지 않게.

### Semantic
- **Success** (`{colors.semantic-success}` — #16a34a) · **Error** (`{colors.semantic-error}` — #dc2626).

## Typography

### Font Family
- **Display**: `EB Garamond` (Light, weight 300) — Waldenburg 라이선스 대체. 둘 다 모더니스트 세리프, 편집 무드 유지.
- **Body / UI**: `Inter` — 데스크톱과 동일.
- **Mono (time / numeric)**: `JetBrains Mono` (Android), `SF Mono` (iOS) — 타임코드 / 재생 위치 표시.

iOS 시스템 SF Pro 는 사용하지 않는다 — 브랜드 톤 일관성을 위해 두 플랫폼 모두 EB Garamond + Inter 번들.

### Font Bundling
- `cmp/src/commonMain/composeResources/font/` 에 `EBGaramond-Light.ttf` (300), `Inter-Regular.ttf` (400), `Inter-Medium.ttf` (500), `Inter-SemiBold.ttf` (600), `JetBrainsMono-Medium.ttf` (500) 배치.
- iOS framework 임베딩 시 `Info.plist` `UIAppFonts` 등록 불필요 (Compose Resources 가 처리).

### Hierarchy (mobile-tuned)

| Token | Size | Weight | Line Height | Letter Spacing | Use |
|---|---|---|---|---|---|
| `{typography.display-hero}` | 40px | 300 | 1.08 | -0.8px | 메인 hero (모바일) — 데스크톱 64px 의 ~62% |
| `{typography.display-xl}` | 32px | 300 | 1.13 | -0.32px | 서브 hero / 큰 화면 타이틀 |
| `{typography.display-lg}` | 28px | 300 | 1.18 | -0.28px | 섹션 헤드 |
| `{typography.display-md}` | 24px | 300 | 1.2 | -0.24px | CTA section / 그룹 타이틀 |
| `{typography.display-sm}` | 20px | 300 | 1.25 | 0 | bottom-sheet 헤더 |
| `{typography.title-lg}` | 18px | 500 | 1.4 | 0 | 카드 타이틀 (Inter) |
| `{typography.title-md}` | 17px | 500 | 1.4 | 0 | top-bar / list section header |
| `{typography.title-sm}` | 15px | 500 | 1.4 | 0.15px | row 라벨 / 강조 캡션 |
| `{typography.body-md}` | 16px | 400 | 1.5 | 0.16px | 기본 body |
| `{typography.body-strong}` | 16px | 500 | 1.5 | 0.16px | 강조 body |
| `{typography.body-sm}` | 14px | 400 | 1.5 | 0.14px | 보조 body / chip |
| `{typography.caption}` | 13px | 400 | 1.4 | 0 | 사진 캡션 / 시간 |
| `{typography.caption-uppercase}` | 11px | 600 | 1.4 | 0.88px | 섹션 라벨 / badge |
| `{typography.button}` | 16px | 500 | 1.0 | 0 | CTA pill (모바일은 14→16px 로 키움) |
| `{typography.tab-label}` | 11px | 500 | 1.2 | 0.11px | 탭바 라벨 |
| `{typography.mono-time}` | 13px | 500 | 1.0 | 0 | 타임코드 / 재생 위치 |

### Principles
- **Display 는 weight 300 고정.** 모바일에서도 절대 bold 금지. 가독성 떨어지면 사이즈 키우기.
- **Body 는 Inter 400/500.** 300 으로 떨어뜨리지 않음 — 모바일 OLED 에서 hairline 처럼 부서짐.
- **Display 는 letter-spacing 음수, body 는 +0.14~0.16px 로 살짝 느슨**. 매거진 dialect.

### iOS / Android 차이
- iOS: Dynamic Type 지원은 **off** — 브랜드 폭이 흔들림. 단 시스템 "Larger Text" 접근성 토글 ON 이면 본문 16/+0.16 → 18/+0.18 로 한 단계만 스케일.
- Android: Material3 typography 와 충돌하지 않게 `MaterialTheme.typography` 와 별개로 `LocalVibiTypography` 제공.

## Layout

### Spacing System
- **Base unit:** 4px.
- **Tokens:** `{spacing.xxs}` 4 · `{spacing.xs}` 8 · `{spacing.sm}` 12 · `{spacing.base}` 16 · `{spacing.md}` 20 · `{spacing.lg}` 24 · `{spacing.xl}` 32 · `{spacing.xxl}` 40 · `{spacing.section-mobile}` 48 · `{spacing.section-tablet}` 64.

### Section Padding
- **모바일 (< 600dp)**: `{spacing.section-mobile}` 48px (vertical) / `{spacing.md}` 20px (horizontal).
- **태블릿 (≥ 600dp)**: `{spacing.section-tablet}` 64px (vertical) / `{spacing.xl}` 32px (horizontal).
- **데스크톱 96px 은 모바일에서 사용 금지** — 한 화면 안에 한 섹션도 안 들어감.

### Safe Area
- iOS: top safe-area + 8px 추가 (notch / Dynamic Island 옆 ink 텍스트가 닿지 않게).
- Android: status bar + 4px.
- Bottom: home indicator + 12px (iOS), nav bar (Android).

### Edge Padding
- Phone: 좌우 20px (`{spacing.md}`).
- Tablet: 좌우 32px (`{spacing.xl}`).

### Whitespace
- 카드 사이 16~20px gap.
- 섹션 내부 항목 가까이(8~16px), 섹션 사이 멀리(48px).
- Atmospheric orb 는 hero 뒤로만; 카드 안에 넣지 않음.

## Elevation & Depth

**Hairline + soft shadow** 시스템. 모바일에서도 그림자 남발 금지 — 카드는 1px hairline 으로 띄움.

| Level | Treatment | Use |
|---|---|---|
| Flat (canvas) | `{colors.canvas}` | 화면 바닥 |
| Card | `{colors.surface-card}` (#ffffff) | feature-card / pricing |
| Hairline border | 1px `{colors.hairline}` | 카드 outline |
| Soft drop | `0 2px 8px rgba(0,0,0,0.04)` | tap 직전 / 셀렉트 (단일 shadow 티어) |
| Bottom-sheet shadow | `0 -4px 16px rgba(0,0,0,0.08)` | bottom-sheet 떠 있을 때 위쪽 그림자 |
| Gradient orb | radial gradient | hero 배경 atmosphere — never card surface |

### Bottom-sheet 깊이
- Scrim: `rgba(0, 0, 0, 0.32)` — 시스템 모달 표준.
- Top corner radius: `{rounded.xxl}` (24px).
- Drag handle: 36×4 막대, `{colors.hairline-strong}`, 상단 중앙 8px 위.

## Shapes

### Border Radius Scale (모바일 튜닝)
모바일은 데스크톱보다 한 단계씩 더 둥글게 — 손가락 접근성 + iOS 시스템 코너 매칭.

| Token | Value | Use |
|---|---|---|
| `{rounded.none}` | 0 | full-bleed 이미지 |
| `{rounded.xs}` | 4 | timeline segment block |
| `{rounded.sm}` | 6 | 자막 overlay |
| `{rounded.md}` | 10 | text input (데스크톱 8 → 10) |
| `{rounded.lg}` | 14 | panel-card (데스크톱 12 → 14) |
| `{rounded.xl}` | 18 | feature-card (데스크톱 16 → 18) |
| `{rounded.xxl}` | 24 | bottom-sheet top / gradient-orb-card |
| `{rounded.pill}` | 9999 | CTA button, badge, chip |
| `{rounded.full}` | 9999 | voice icon, avatar, icon-button |

## Components

### Top Bar
**`top-bar`** — 56px 높이. 배경 `{colors.canvas}`, 텍스트 `{colors.ink}` `{typography.title-md}`. 좌측 아이콘(< back 또는 햄버거), 중앙 제목, 우측 액션 1개. 그림자 없음 — `{colors.hairline-soft}` 1px 하단 라인만.

### Tab Bar (Bottom Navigation)
**`tab-bar`** — 64px 높이 + bottom safe-area. 배경 `{colors.canvas-soft}`, 상단 1px `{colors.hairline}`. 항목 4개 (홈 / 보이스 / 프로젝트 / 설정). 활성 탭은 아이콘 + 라벨 모두 `{colors.ink}`, 비활성 `{colors.muted}`. 라벨 typography `{typography.tab-label}`.

iOS 패턴: blur background 없음 — ElevenLabs 의 평평한 평면 미학 유지.

### Buttons

**`button-primary`** — 잉크 pill. `{colors.primary}` 배경, `{colors.on-primary}` 텍스트, `{typography.button}` (16/500), 패딩 14×24, **높이 48px**, `{rounded.pill}`. iOS 44pt 최소 + 4pt 여유.

**`button-primary-pressed`** — 누름 상태. `{colors.primary-active}`. 스케일 변형 없음 — 색만 변화.

**`button-outline`** — 1px `{colors.hairline-strong}` border, 투명 배경. 같은 높이 48px.

**`button-text`** — 인라인 텍스트 액션. 최소 44px 터치 타깃 보장 (visual height 가 작아도 invisible padding).

**`icon-button`** / **`icon-button-filled`** — 44×44px (iOS HIG). 아이콘 자체는 24px.

### Hero & Atmospheric

**`hero-section`** — 첫 화면. 배경 `{colors.canvas}`, headline `{typography.display-hero}` (40/300/-0.8), subhead `{typography.body-md}`, CTA 1~2개. 뒤에 atmospheric orb 1개 (mint 또는 peach 디폴트). Padding 32×20.

**`gradient-orb-card`** — soft radial gradient orb 가 깔린 카드. 배경 `{colors.canvas-soft}`, `{rounded.xxl}` (24), padding 24. 5종 variant (mint/peach/lavender/sky/rose).

모바일 구현 (Compose):
```kotlin
Brush.radialGradient(
    colors = listOf(
        VibiColors.gradientMint.copy(alpha = 0.6f),
        VibiColors.gradientMint.copy(alpha = 0.0f)
    ),
    radius = 240.dp.toPx()
)
```
다크 모드에선 alpha 0.6 → 0.3.

**`audio-waveform-card`** — 파형 카드. 배경 `{colors.surface-card}`, `{rounded.xl}`, padding 16. play 버튼 + 파형 + 보이스 메타.

### Cards & Lists

**`feature-card`** — 1-up 또는 2-up. `{colors.surface-card}`, `{rounded.xl}`, padding 20, 1px `{colors.hairline}` border. 모바일 padding 24 → 20 으로 축소.

**`panel-card`** — 카드 안의 sub-panel. `{colors.canvas-soft}` 바탕, `{rounded.lg}`, padding 16. 더 약한 계층.

**`voice-row`** — 보이스 리스트 row. 좌측 40px circular icon, 가운데 이름+악센트 stack, 우측 preview 버튼. 패딩 12×16, 최소 64px (터치 타깃). 하단 1px `{colors.hairline-soft}` 디바이더.

**`voice-icon-circular`** — `{colors.surface-strong}` 바탕, `{rounded.full}`, **40px** (데스크톱 32 → 모바일 40 — 핑거 타깃 + 비주얼 weight).

### Forms & Tags

**`text-input`** — `{colors.surface-card}` 바탕, `{rounded.md}` (10), padding 14×16, **높이 52px** (44pt 초과). focus 시 border 2px `{colors.ink}`.

**`badge-pill`** — `{colors.surface-strong}`, `{typography.caption-uppercase}`, padding 4×10.

**`chip`** — 인터랙티브 chip (필터 / 선택). `{colors.surface-strong}`, `{rounded.pill}`, padding 8×14, **최소 36px**. 선택 상태는 배경 `{colors.ink}` + 텍스트 `{colors.on-primary}` 으로 invert.

**`chip-disabled`** — `{colors.hairline-soft}` 배경, `{colors.muted-soft}` 텍스트.

### Bottom Sheet (vibi 핵심 패턴)

**`bottom-sheet`** — vibi 의 6개 bottom-sheet (자막/더빙/믹서/녹음/렌더/설정) 가 핵심 인터랙션. 배경 `{colors.surface-card}`, top corners `{rounded.xxl}` (24), padding 20. 상단에 36×4 drag handle (`{colors.hairline-strong}`).

상태:
- **peek**: 화면 1/3 (mini-player 만 노출).
- **half**: 화면 1/2 (주 컨트롤).
- **expanded**: 화면 7/8 (전체 옵션 + safe-area).

스크림은 expanded 상태에서만 활성 (`rgba(0,0,0,0.32)`).

### Subtitle Overlay (영상 위 자막)

**`subtitle-overlay`** — 비디오 위 자막 박스. 배경 `{colors.subtitle-overlay-bg}` (잉크 80% alpha), 텍스트 `{colors.on-dark}`, `{rounded.sm}`, padding 6×10. 비디오 하단 16px 안쪽.

### Timeline Bar (편집 화면 핵심)

vibi 통합 타임라인 — 4 종 색 변형:

**`timeline-bar-track`** — 4px 높이 트랙. `{colors.timeline-bar-track}` (#e7e5e4).

**`timeline-bar-segment`** — 기본 segment 블록. `{colors.timeline-bar-segment}` (#a8a29e, 중성 회색).

**`timeline-bar-segment-edited`** — 편집 적용 segment. `{colors.timeline-bar-segment-edited}` (#c8b8e0, lavender — atmosphere 팔레트 차용). volumeScale ≠ 1 / speedScale ≠ 1 / trim / duplicatedFrom 중 하나라도 있으면.

**`timeline-bar-directive`** — 음성분리 directive 블록. `{colors.timeline-bar-directive}` (#f4c5a8, peach). edited 와 hue 분리 (lavender vs peach) — 사용자가 적용한 분리 구간.

이 4색만이 atmospheric 팔레트가 **장식 외 용도** 로 등장하는 유일한 케이스. Timeline 은 vibi 도메인 핵심이라 예외적으로 색을 의미적으로 사용.

### CTA & Snackbar

**`cta-section`** — 페이지 하단 CTA 밴드. 배경 `{colors.canvas}`, headline `{typography.display-md}` (24/300), 잉크 pill 1개. Padding 48×20.

**`snackbar`** — 시스템 토스트. 배경 `{colors.surface-dark}` (#0c0a09), 텍스트 `{colors.on-dark}`, `{typography.body-sm}`, `{rounded.md}`, padding 14×16. 하단 safe-area + 8px.

### Misc

**`list-divider`** — 1px `{colors.hairline-soft}`. List row 사이.

## Do's and Don'ts

### Do
- `{colors.primary}` (잉크 pill) 은 primary CTA 한정.
- EB Garamond Light 300 으로 모든 display headline. 모바일에서도 절대 bold 금지.
- Inter +0.14~0.16px tracking 으로 body — 매거진 dialect.
- 파스텔 그라디언트 orb 는 atmosphere 한정. Timeline 4색은 도메인 의미 있는 유일한 예외.
- 모든 CTA / chip / badge 에 pill 모양.
- 터치 타깃 최소 44pt (iOS) — 시각적으로 작아도 invisible padding 으로 보장.
- Bottom-sheet top corner `{rounded.xxl}` (24).

### Don't
- Saturated 브랜드 액션 컬러 도입 금지. 잉크 pill 만 CTA 컬러.
- Display 를 bold 로 만들지 말 것 — 편집 voice 가 소비자-마케팅 voice 로 변함.
- Gradient orb 를 버튼 fill / 텍스트 컬러 / 카드 배경으로 쓰지 말 것 (timeline 4색 예외).
- CTA 에 `{rounded.none}` 금지. Pill geometry 가 브랜드.
- Body Inter 를 weight 300 으로 떨어뜨리지 말 것 — display 매칭하려고 그러면 안 됨, OLED 에서 부서짐.
- iOS 시스템 SF Pro 로 자동 fallback 금지 — EB Garamond + Inter 강제 번들.
- 다크 모드에서 gradient orb alpha 그대로 두지 말 것 (60% → 30%).
- Material3 elevation tonal overlay 사용 금지 — hairline + soft shadow 만.
- 데스크톱 96px section padding 모바일 적용 금지 — 48px / 64px 만.
- 한 화면에 orb 2개 이상 금지 — GPU + 시각 노이즈.

## Responsive Behavior

### Breakpoints

| Name | Width | Key Changes |
|---|---|---|
| Compact phone | < 360dp | display-hero 40 → 32; tab-bar 라벨 숨김 (아이콘만); hero orb 축소 50% |
| Phone | 360–600dp | 기본 토큰 — 본 문서가 기술. |
| Tablet | 600–840dp | section-padding 48 → 64; feature-card 1-up → 2-up; display-hero 40 → 48 |
| Large tablet / Foldable | > 840dp | content max-width 720dp 캡; 좌우 여백 자동 분배 |

### Touch Targets
- 모든 인터랙티브 요소 최소 44×44pt (iOS HIG) / 48×48dp (Material).
- Pill button 48px ✓.
- Voice icon 40px → row 자체 64px (icon 양쪽 padding 으로 64 확보) ✓.
- Chip 36px → padding 으로 44px tap 영역 ✓.
- Icon button 44×44 ✓.

### Collapsing Strategy
- Top bar 액션 3개 초과 → "..." overflow.
- Feature 카드: 2-up → 1-up (< 600dp).
- Tab bar 라벨: < 360dp 에선 아이콘만.
- Bottom-sheet expanded 상태에서도 키보드 가리지 않게 IME inset 추적.

### Orientation
- Landscape: bottom-sheet 는 측면 sheet 로 전환 (iPad / 폴더블 가로).
- Phone landscape: hero orb 비활성, display-hero 32px 로 축소.

## Mobile-Specific Adaptations

데스크톱 ElevenLabs 시스템에 없는 모바일 신규 / 변경 항목 요약:

| 항목 | 데스크톱 | 모바일 | 이유 |
|---|---|---|---|
| display-hero | 64px | 40px | 화면 폭 한계 |
| section padding | 96px | 48px (phone) | 한 화면 밀도 |
| pill 높이 | 40px | 48px | iOS 44pt HIG |
| pill typography | 15px | 16px | 손가락 가독성 |
| voice-icon | 32px | 40px | 핑거 타깃 + visual weight |
| text-input 높이 | 44px | 52px | 키보드 + 핑거 |
| feature-card padding | 24px | 20px | 좁은 폭 |
| Waldenburg | 사용 | EB Garamond | 라이선스 + KMP 번들 가능 |
| 신규 컴포넌트 | — | tab-bar / bottom-sheet / subtitle-overlay / timeline-bar-* / chip / snackbar / icon-button | iOS/Android 네이티브 패턴 |
| Atmosphere orb 개수 | 다수 | 1/화면 | GPU 부하 |

### iOS-First 우선순위
vibi 는 iOS 먼저 출시. Compose Multiplatform iOS 빌드 시 다음을 사전 검증:
- EB Garamond / Inter / JetBrains Mono ttf 가 iOS framework 에 임베드되는지 (`compileResources` task 출력 확인).
- Bottom-sheet drag gesture 가 iOS 에서 시스템 swipe-back 과 충돌 안 하는지.
- `Brush.radialGradient` orb 가 iOS Metal renderer 에서 60fps 유지하는지.
- Safe-area 가 notch / Dynamic Island / home indicator 모두 회피하는지.

## Token → Compose 매핑 가이드

`:cmp` 의 `VibiTheme.kt` 를 ElevenLabs 토큰으로 갈아끼울 때:

```kotlin
// commonMain/.../theme/VibiColors.kt
data class VibiColors(
    val canvas: Color,           // {colors.canvas}
    val canvasSoft: Color,       // {colors.canvas-soft}
    val canvasDeep: Color,       // {colors.canvas-deep}
    val surfaceCard: Color,      // {colors.surface-card}
    val surfaceStrong: Color,    // {colors.surface-strong}
    val ink: Color,              // {colors.ink}
    val body: Color,             // {colors.body}
    val muted: Color,            // {colors.muted}
    val mutedSoft: Color,        // {colors.muted-soft}
    val hairline: Color,         // {colors.hairline}
    val hairlineSoft: Color,     // {colors.hairline-soft}
    val hairlineStrong: Color,   // {colors.hairline-strong}
    val primary: Color,          // {colors.primary}
    val onPrimary: Color,        // {colors.on-primary}
    // Atmosphere
    val gradientMint: Color,
    val gradientPeach: Color,
    val gradientLavender: Color,
    val gradientSky: Color,
    val gradientRose: Color,
    // vibi domain
    val subtitleOverlayBg: Color,
    val timelineBarTrack: Color,
    val timelineBarSegment: Color,
    val timelineBarSegmentEdited: Color,
    val timelineBarDirective: Color,
    val chipBg: Color,           // = surfaceStrong
    val chipBgDisabled: Color,   // = hairlineSoft
    val chipContentDisabled: Color, // = mutedSoft
)
```

기존 `VibiColors` 의 timeline / chip / subtitle 토큰은 이름만 보존, 색만 ElevenLabs 팔레트로 교체. **데이터 클래스 모양 유지** → 호출처(`LocalVibiColors.current.chipBg`) 변경 0.

다크 인스턴스는 `dark-canvas` / `dark-surface-*` / `dark-ink` / `dark-body` 토큰으로 매핑.

## Known Gaps

- **Waldenburg** 정식 사용은 라이선스 필요. 본 문서는 EB Garamond Light 를 1차 대체로 명시. GT Sectra 가 더 가깝지만 유료 — EB Garamond (OFL) 가 KMP 번들에 안전.
- **애니메이션 타이밍** (orb drift, waveform pulse, bottom-sheet spring) — 별도 motion spec 필요.
- **Compose Multiplatform** iOS 폰트 로딩이 첫 frame 에서 system fallback 으로 깜빡임 → 스플래시에서 사전 로드 필요 (별도 task).
- **접근성** (VoiceOver / TalkBack) 라벨, 다이내믹 텍스트 스케일, contrast 검증 — 본 문서 scope 외.
- **Form validation** (인라인 에러 메시지 위치 / 색) — semantic-error 토큰만 정의, 패턴 미정의.
- **Empty state / Loading state** 일러스트레이션 — atmosphere orb 활용 가이드 미작성.
