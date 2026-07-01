package com.vibi.cmp.platform

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.vibi.shared.data.local.db.VibiDatabase
import com.vibi.shared.data.local.db.applicationContext
import com.vibi.shared.platform.stripFileScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** picker 직후 아직 Segment 로 커밋되지 않은 in-flight 복사본을 보호하는 유예창(5분). */
private const val GRACE_MILLIS = 5L * 60L * 1000L

/**
 * `filesDir/picker_media/<uuid>/<원본 파일명>` 누수 청소기 (androidMain 전용).
 *
 * 배경: [com.vibi.cmp.platform.MediaPicker] 의 copyPickedToFiles 가 picker 영상을 위 경로로
 * 복사하지만, commonMain 의 cascadeDeleteProject 는 DB row(segments / bgm_clips)만 지우고
 * 복사본 파일은 남긴다(iOS 도 동일). filesDir 는 OS 가 비우지 않으므로 picker_media 가 무한
 * 누적된다.
 *
 * 동작: 어떤 segment / bgm_clip 에서도 더 이상 참조되지 않는("orphan") uuid 서브디렉터리를
 * 찾아 통째로 삭제한다. 단, 방금 picker 로 복사됐지만 아직 Segment 로 커밋되지 않은 in-flight
 * 복사본을 지우지 않도록 [GRACE_MILLIS] 유예창보다 오래된(=마지막 수정 후 5분 경과) 디렉터리만
 * 대상으로 한다.
 *
 * 안전장치: 참조 경로(live set)를 DB 에서 한 번이라도 못 읽으면(파일 부재 / 쿼리 실패) 삭제를
 * 전혀 수행하지 않는다 — "참조 0건(정상 빈 결과)" 과 "읽기 실패" 를 구분해, 일시적 실패로 사용
 * 중인 파일을 지우는 사고를 막는다. 전체를 runCatching 으로 감싸 어떤 예외도 startup 을 막지
 * 않는다.
 *
 * Room 핸들을 직접 쓰지 않는 이유: VibiDatabase 는 BundledSQLiteDriver 로 생성돼
 * `openHelper`(SupportSQLiteOpenHelper)가 없고, room-runtime 도 :shared 에서 implementation 이라
 * :cmp 컴파일 클래스패스에 노출돼 있지 않다. 그래서 Room 이 쓰는 동일한 DB 파일
 * (`applicationContext.getDatabasePath(DB_FILE_NAME)`)을 Android 프레임워크 SQLiteDatabase 로
 * 열어 raw SQL 로 전(全) 사용자 참조를 모은다. startup fire-and-forget 시점엔 Room 이 아직
 * lazy-open 전이라 보통 단독 접근이다.
 */
suspend fun sweepOrphanPickerMedia() {
    withContext(Dispatchers.IO) {
        runCatching {
            val context = applicationContext
            val root = File(context.filesDir, "picker_media")
            if (!root.isDirectory) return@runCatching

            val cutoff = System.currentTimeMillis() - GRACE_MILLIS

            // 1) DB 를 열기 전에 시간 기준 후보만 먼저 추린다. 각 서브엔트리를 단일 walk 로 순회하며
            //    포함 파일들의 절대경로와 최신 수정시각을 동시에 계산하고(이중 순회 회피), 유예창보다
            //    오래된(newest < cutoff) 것만 삭제 후보로 남긴다.
            val candidates = root.listFiles()?.mapNotNull { entry ->
                var newest = entry.lastModified()
                val paths = ArrayList<String>()
                val files = if (entry.isDirectory) entry.walkTopDown().filter { it.isFile } else sequenceOf(entry)
                for (file in files) {
                    paths.add(file.absolutePath)
                    val m = file.lastModified()
                    if (m > newest) newest = m
                }
                if (newest < cutoff) SweepCandidate(entry, paths) else null
            }.orEmpty()

            // 후보가 없으면 DB 를 열지 않고 종료 — orphan 판정용 참조 집합 조회 비용을 아낀다.
            if (candidates.isEmpty()) return@runCatching

            // 실패하면 예외가 전파돼 아래 삭제 루프가 아예 실행되지 않는다(= 아무것도 안 지움).
            val liveSet = readReferencedFilePaths(context)

            candidates.forEach { candidate ->
                // 디렉터리 내 어떤 파일이라도 segment / bgm_clip 에서 참조되면 보존.
                if (candidate.filePaths.any { liveSet.contains(it) }) return@forEach
                candidate.dir.deleteRecursively()
            }
        }
    }
}

/** 삭제 후보 서브엔트리: 통째로 지울 [dir] 과, live 참조 대조에 쓸 포함 파일들의 절대경로. */
private class SweepCandidate(val dir: File, val filePaths: List<String>)

// 아래 테이블/컬럼 리터럴은 Room 엔티티(SegmentEntity / BgmClipEntity)와 반드시 일치해야 한다 —
// raw SQL 이라 컴파일 타임 검증이 없으므로 스키마 변경 시 함께 갱신할 것.
private const val TABLE_SEGMENTS = "segments"
private const val TABLE_BGM_CLIPS = "bgm_clips"
private const val COL_SEGMENT_SOURCE_URI = "sourceUri"
private const val COL_BGM_SOURCE_URI = "sourceUri"
private const val COL_BGM_ORIGINAL_SOURCE_URI = "originalSourceUri"
private const val COL_BGM_VOICE_ONLY_URI = "voiceOnlyUri"

/** 참조 경로를 모을 (테이블, 컬럼) 소스. 각 arm 은 독립적으로 조회·guard 된다. */
private val REFERENCE_SOURCES = listOf(
    TABLE_SEGMENTS to COL_SEGMENT_SOURCE_URI,
    TABLE_BGM_CLIPS to COL_BGM_SOURCE_URI,
    TABLE_BGM_CLIPS to COL_BGM_ORIGINAL_SOURCE_URI,
    TABLE_BGM_CLIPS to COL_BGM_VOICE_ONLY_URI,
)

/**
 * segments + bgm_clips 의 모든 소스 경로를 (전 사용자) 모아 정규화된 절대경로 Set 으로 반환.
 *
 * DB 파일을 못 열면 예외를 던진다 — 호출부([sweepOrphanPickerMedia])가 그 경우 삭제를 건너뛰도록
 * 하기 위함이다(빈 Set 으로 fallback 하지 않는다). 단 개별 (테이블,컬럼) arm 은 각각 guard 되어,
 * 한 컬럼이 리네임/삭제되어 실패해도 나머지 arm 은 계속 모은다(전체 sweep 이 죽지 않도록).
 */
private fun readReferencedFilePaths(context: Context): Set<String> {
    val dbFile = context.getDatabasePath(VibiDatabase.DB_FILE_NAME)
    // DB 가 아직 없으면 참조 집합을 신뢰성 있게 구성할 수 없음 → 던져서 삭제 스킵.
    check(dbFile.exists()) { "Vibi DB not found: ${dbFile.path}" }

    // SELECT 만 수행하므로 read-only 가 의도. 단 BundledSQLiteDriver 가 WAL 을 쓰면 OPEN_READONLY 가
    // -shm/-wal 갱신 불가로 실패할 수 있어, 실패 시 READWRITE 로 폴백한다(데이터는 바꾸지 않음).
    val db = runCatching {
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
    }.getOrElse {
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
    }
    try {
        val result = HashSet<String>()
        for ((table, column) in REFERENCE_SOURCES) {
            // arm 별 guard: 한 컬럼/테이블이 스키마 변경으로 조회 실패해도 나머지는 계속 수집.
            runCatching {
                db.rawQuery("SELECT $column FROM $table", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        if (cursor.isNull(0)) continue
                        val raw = cursor.getString(0) ?: continue
                        normalizeToAbsolutePath(raw)?.let(result::add)
                    }
                }
            }
        }
        return result
    } finally {
        db.close()
    }
}

/**
 * 저장된 소스 URI 문자열을 파일시스템 절대경로로 정규화.
 * - `file://` scheme 제거 + %-인코딩 디코드는 shared 의 [stripFileScheme] 재사용 — MediaPicker 는
 *   `Uri.fromFile` 로 공백 등을 `%20` 인코딩해 저장하지만, `File.absolutePath` 는 디코드된 경로라
 *   비교 전 디코드가 필요하다.
 * - 절대경로(`/` 시작)만 반환 — content:// 등 비(非)파일 URI 는 picker_media 와 무관하므로 null.
 */
private fun normalizeToAbsolutePath(raw: String): String? {
    val s = raw.trim()
    if (s.isEmpty()) return null
    val path = stripFileScheme(s)
    if (!path.startsWith("/")) return null
    return File(path).absolutePath
}
