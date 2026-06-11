package com.vibi.shared.domain.util

import com.vibi.shared.domain.model.Stem

/**
 * BFF 가 path-only(`/api/v2/...`) 미디어/stem URL 을 돌려줄 때 [baseUrl] 과 join 해 absolute URL 로.
 * 이미 absolute(http…)면 그대로. iOS AVPlayer/AVAudioPlayer 가 host 없는 URL 을 silent fail 처리하는 것 회피.
 */
fun absoluteMediaUrl(baseUrl: String, pathOrUrl: String): String =
    if (pathOrUrl.startsWith("http")) pathOrUrl
    else "${baseUrl.trimEnd('/')}/${pathOrUrl.trimStart('/')}"

/** [Stem.url] 을 [baseUrl] 기준 absolute 로 보정한 복사본. 이미 absolute 면 그대로 반환. */
fun Stem.withAbsoluteUrl(baseUrl: String): Stem {
    val abs = absoluteMediaUrl(baseUrl, url)
    return if (abs == url) this else copy(url = abs)
}
