# CLAUDE.md — `:shared` 모듈

## Project

vibi `:shared` — **Kotlin Multiplatform 비즈니스 로직 모듈**. 도메인 모델 + 리포지토리(인터페이스+구현) + Ktor Client multiplatform BFF 클라이언트 + Room v19 (multiplatform) + UseCase + ViewModel + Koin DI 를 `commonMain` 에 두고 `androidMain`/`iosMain` 으로 플랫폼 의존을 분리한다. UI 코드는 형제 모듈 `:cmp` 가 본 모듈을 소비한다.

- **gradle 모듈명**: `:shared` (gradle 루트는 `vibi-mobile/`, `include(":shared")`)
- 코드 스타일: official
- `org.gradle.configuration-cache=true` (일부 명령은 `--no-configuration-cache` 필요 — `.claude/skills/build.md` 참조)
- 코드 패키지는 아직 `com.vibi.shared.*` (Room `VibiDatabase` + iOS framework 이름 영향). 폴더·docs 는 `vibi` 로 통일하되 패키지 마이그레이션은 별도 작업.

> 워크스페이스 지도와 형제 모듈(`:cmp`, `iosApp`) 관계는 워크스페이스 루트 `CLAUDE.md` 참조.

## BFF_BASE_URL 타깃별 주의

`vibi-mobile/local.properties` 의 `BFF_BASE_URL` 단일 값을 Android/iOS 양쪽에서 쓴다. 타깃별로 필요한 주소가 다르므로 네트워크 클라이언트 구현 시 `expect val bffBaseUrl: String` 로 `androidMain`/`iosMain` 에서 각각 다르게 주입하거나 빌드 플레이버로 분기.

- Android 에뮬레이터 → `http://10.0.2.2:8080/`
- Android 실기기 → 맥 LAN IP
- iOS 시뮬레이터 → `http://localhost:8080/` 또는 맥 LAN IP
- iOS 실기기 → 맥 LAN IP

## 현재 구조

```
shared/
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/com/vibi/shared/
    │   ├── domain/
    │   │   ├── model/                       # DubClip, Segment, Stem, EditProject,
    │   │   │                                # SubtitleClip, BgmClip, ImageClip, TextOverlay,
    │   │   │                                # VideoInfo, ImageInfo, ValidationResult, ... (15 files)
    │   │   ├── repository/                  # 인터페이스 (AudioSeparationRepository,
    │   │   │                                #         AutoDubRepository, AutoSubtitleRepository, ...)
    │   │   ├── usecase/                     # 카테고리 (input/subtitle/separation/timeline/text/bgm/image/save/draft/export)
    │   │   └── util/                        # LanePacking, ColorValidation
    │   ├── data/
    │   │   ├── remote/api/BffApi.kt         # Ktor Client multiplatform — 12 v2 엔드포인트 + lipsync
    │   │   ├── remote/dto/                  # kotlinx.serialization DTO
    │   │   ├── repository/                  # 인터페이스 구현 (Room + BFF)
    │   │   └── local/db/                    # Room v19 (VibiDatabase, 7 entity + 7 DAO + Migrations)
    │   ├── ui/                              # ViewModel (InputVM, TimelineVM, ExportVM, ShareVM, ChatVM)
    │   ├── domain/chat/                     # ChatToolDispatcher · ProjectContextBuilder (Gemini routing)
    │   ├── platform/                        # FileSystem expect, currentTimeMillis 등
    │   └── di/                              # Koin 모듈 (database/network/repository/usecase/viewmodel)
    ├── androidMain/                         # AndroidVideoMetadataExtractor, AndroidGallerySaver,
    │                                        # AutoDubRepositoryImpl, AutoSubtitleRepositoryImpl,
    │                                        # MediaJobUploader, AndroidExportPlatformAdapter,
    │                                        # androidPlatformModule
    ├── iosMain/                             # IosVideoMetadataExtractor, IosGallerySaver,
    │                                        # IosAutoDubRepositoryImpl, IosAutoSubtitleRepositoryImpl,
    │                                        # IosMediaJobUploader, IosExportPlatformAdapter,
    │                                        # iosPlatformModule
    └── commonTest/                          # BffApiTest (13), MigrationsTest, Migration7To8Test (4)
```

## 중요 제약

- `commonMain` 에 **Android/JVM 전용 API 금지** (`android.*`, `java.io.File`, `java.util.UUID`, `System.currentTimeMillis`). 플랫폼 의존(파일시스템·오디오·메타데이터·갤러리)은 `expect fun` 또는 platform adapter interface (예: `ExportPlatformAdapter`).
- Retrofit / Moshi / OkHttp / Hilt 는 iOS 불가 — 네트워크는 Ktor Client multiplatform, 직렬화는 kotlinx.serialization, DI 는 Koin.
- 본 모듈은 **로직만**. UI 코드(`@Composable`)는 `cmp/` 로.
- 본 모듈이 모바일 도메인의 단일 source of truth — legacy-android 시절의 모델은 모두 흡수됐다.

## 아키텍처·Flow 규약

도메인·리포지토리 설계 시 따른다. 과도한 추상화로 boilerplate 만 늘리지 않기 위함.

- **엄격한 클린 아키텍처 강제 안 함** — UseCase / Repository / DataSource 풀세트 대신 service 래퍼 한 겹으로 충분. 도메인 모델이 다중 데이터소스를 가로지를 때만 한 겹 더.
- **모든 앱에 도메인 레이어 필요 없음** — 비즈니스 로직이 화면과 1:1 이면 ViewModel 직결. 도메인 레이어는 "여러 화면이 같은 비즈니스 규칙 공유" 또는 "데이터 소스 추상화 필요" 일 때만 가치 있음. 안 그러면 매핑 boilerplate.
- **리포지토리 함수가 일회성이면 Flow 반환 금지** — `suspend fun login(): Result<User>` 가 `Flow<Result<User>>` 보다 명확. Flow 는 "값이 시간에 따라 바뀜" 의미라 일회성에 쓰면 의미 오염 + collect 강제·취소 처리·디바운스 부담.
- **Flow 반환 함수는 보통 non-suspend** — `fun observeUser(): Flow<User>` 는 cold flow 빌더라 호출 시점에 작업 시작 안 함. `suspend fun observeUser(): Flow<User>` 는 호출자가 collect 도 못 시작하는 어색한 API.

## Timeline stepper 동작 규약

`TimelineViewModel` 의 3단계 stepper (`Edit` → `AudioSources` → `SubtitleDub`) 전환 시 적용되는 비대칭 규약. 코드 곳곳에 흩어져 있어 한눈에 안 잡히므로 명시.

- **단계 이동은 산출물 wipe 안 함** — `onAdvanceStep` / `onRequestStepBack` 양방향 모두 `currentStep` 만 변경. BGM/separation/자막/더빙 결과는 다른 단계 갔다 와도 그대로 유지. 사용자가 잠깐 다른 단계 들렀다가 돌아오는 흐름이 일반적이므로 보존이 기본.
- **`commitSegmentEdit` 만 산출물 wipe** — 사용자가 영상편집 모드의 ✓로 segment 자체를 바꿨을 때만 `resetTimelineDerivedResults()` 가 BGM/separation/자막/더빙을 정리. segment 가 바뀌면 downstream 산출물이 stale 이라 어쩔 수 없음.
- **Undo/redo 는 단계별 분리** — `mainUndoManagersByStep: Map<TimelineStep, UndoRedoManager>` 로 step 마다 독립 스택. **forward 이동 시 출발 단계 스택 유지**, **backward 이동 시 출발 단계 스택 초기화**. 즉 "앞으로 가다 돌아와도 거기서 이어 undo 가능, 뒤로 갔다가 다시 앞으로 가면 새 시작". `activeUndoManager()` 는 `isSegmentEditMode` 시 별도 `editModeUndoRedoManager`, 아니면 currentStep 의 매니저 반환.
- **BGM mux 시점은 lazy** — stepper 이동 시점이 아니라 자막/더빙 생성 버튼 클릭 시 `EnsureLatestRenderUseCase` 가 BFF 에 BGM/separation/image/text 포함한 단일 영상 render 잡 제출. stepper UX 만 보고 "이동 시 합성됨" 으로 오해 금지.

## Skills

- `build` — `.claude/skills/build.md`. KMP 빌드 명령과 configuration-cache 주의.
- `ios-kn-patterns` — `.claude/skills/ios-kn-patterns.md`. iOS Kotlin/Native 자주 만나는 버그 패턴 모음 (NSURL, AVAsset, AVPlayer, K/N cinterop, NSData↔ByteArray, AVMutableComposition, CMP UIKitView). iosMain 또는 cmp/iosMain 코드 쓸 때 참조. 새 버그 만나면 본 skill 에 추가.
