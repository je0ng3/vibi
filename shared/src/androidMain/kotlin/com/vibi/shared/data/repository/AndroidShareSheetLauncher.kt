package com.vibi.shared.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.vibi.shared.domain.usecase.share.ShareSheetLauncher
import com.vibi.shared.platform.ActivityProvider
import com.vibi.shared.platform.stripFileScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android share sheet — `Intent.createChooser` 로 시스템 공유 UI 표시.
 *
 * 시스템이 설치된 인스타/카톡/메시지/Drive/메일 등을 자동 표시 (iOS
 * [IosShareSheetLauncher] 의 `UIActivityViewController` 동등물).
 *
 * 각 경로를 content [Uri] 로 매핑:
 *  - `content://` 는 그대로 사용.
 *  - `file://` 또는 절대경로는 [FileProvider.getUriForFile] 로 grant 가능한
 *    `content://` 로 변환 (authority = `${packageName}.fileprovider`,
 *    `:cmp` AndroidManifest 의 FileProvider + `res/xml/file_paths.xml` 사용).
 *
 * 단일 영상은 [Intent.ACTION_SEND], 다중 영상은 [Intent.ACTION_SEND_MULTIPLE].
 * [Intent.FLAG_GRANT_READ_URI_PERMISSION] 으로 수신 앱에 임시 read 권한 부여.
 *
 * 일부 외부 앱은 다중 첨부 미지원이라 첫 번째만 받거나 에러일 수 있음 (앱별 동작 차이).
 */
class AndroidShareSheetLauncher(
    private val context: Context,
    private val activityProvider: ActivityProvider,
) : ShareSheetLauncher {

    override suspend fun shareVideo(
        sourcePath: String,
        mimeType: String,
        title: String?,
    ): Result<Unit> = shareVideos(
        sourcePaths = listOf(sourcePath),
        mimeType = mimeType,
        title = title,
    )

    override suspend fun shareVideos(
        sourcePaths: List<String>,
        mimeType: String,
        title: String?,
    ): Result<Unit> = runCatching {
        require(sourcePaths.isNotEmpty()) { "sourcePaths must not be empty" }

        val uris: List<Uri> = sourcePaths.map { path -> toContentUri(path) }
        require(uris.isNotEmpty()) { "no resolvable paths in $sourcePaths" }

        val shareIntent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
        }.apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            title?.let { putExtra(Intent.EXTRA_TITLE, it) }
        }

        val chooser = Intent.createChooser(shareIntent, title)

        withContext(Dispatchers.Main) {
            val activity = activityProvider.current
            if (activity != null) {
                activity.startActivity(chooser)
            } else {
                context.startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }

        Unit
    }

    private fun toContentUri(path: String): Uri =
        if (path.startsWith("content://")) {
            Uri.parse(path)
        } else {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(stripFileScheme(path)),
            )
        }
}
