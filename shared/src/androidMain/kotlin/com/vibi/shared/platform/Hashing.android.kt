package com.vibi.shared.platform

actual suspend fun sha256HexOfFile(path: String): String =
    throw NotImplementedError(
        "Android asset upload not implemented yet — Android render uses v2 multipart path"
    )

actual suspend fun statFile(path: String): FileStat =
    throw NotImplementedError(
        "Android asset upload not implemented yet — Android render uses v2 multipart path"
    )
