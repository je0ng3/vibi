package com.vibi.shared.ui.export

import android.content.Context

/**
 * v1 stub — vibi 는 iOS 우선 배포라 Android 런타임 사용 X.
 * AndroidPlatformModule 에서 [ExportPlatformAdapter] 의 single 구현으로 등록되므로 클래스/시그니처만 유지.
 */
class AndroidExportPlatformAdapter(
    @Suppress("unused") private val context: Context,
) : ExportPlatformAdapter {

    override suspend fun executeExport(
        request: ExportRequest,
        onProgress: (percent: Int) -> Unit,
    ): Result<String> = error("Android not supported in v1")
}
