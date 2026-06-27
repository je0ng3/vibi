package com.vibi.cmp.ui.account

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.vibi.cmp.theme.LocalVibiColors
import com.vibi.cmp.theme.LocalVibiTypography

/**
 * 무료 분리 체험량을 모두 사용했을 때, 잔액 부족 화면에 표시하는 중립 안내.
 *
 * 앱은 완전 무료 — 크레딧은 인앱·웹·타 플랫폼 어디서도 판매하지 않는다. 따라서 "더 받기 /
 * 구매 / coming soon / 알려달라" 같이 유료 콘텐츠 획득을 암시하는 표현은 일절 두지 않는다.
 * App Store 3.1.1(외부에서 구매한 디지털 콘텐츠를 IAP 없이 접근) 오해를 사지 않도록, 무료
 * 사용 한도에 도달했다는 사실만 담담히 알린다 — 행동 유도(CTA)·수요 수집 없음.
 */
@Composable
fun FreeCreditsUsedNote(modifier: Modifier = Modifier) {
    val typo = LocalVibiTypography.current
    val tokens = LocalVibiColors.current
    Text(
        text = "You've used all your free separations.",
        style = typo.bodyMd.copy(fontWeight = FontWeight.SemiBold),
        color = tokens.onBackgroundPrimary,
        modifier = modifier,
    )
}
