package com.vibi.cmp.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibi.cmp.platform.isIosPlatform
import com.vibi.cmp.theme.LocalVibiColors
import com.vibi.cmp.theme.LocalVibiTypography
import com.vibi.cmp.ui.auth.AppleSignInButton
import com.vibi.cmp.ui.auth.GoogleSignInButton
import com.vibi.cmp.ui.components.VibiChipRow
import com.vibi.cmp.ui.components.VibiDialog
import com.vibi.cmp.ui.components.VibiPrimaryButton
import com.vibi.shared.domain.model.IdentityProvider
import com.vibi.shared.domain.model.LinkedIdentity
import com.vibi.shared.ui.account.UserMenuViewModel
import com.vibi.shared.ui.account.UserMenuViewModel.LinkMessage

/**
 * '계정 연결' 서브시트 — [UserMenuSheet] 의 "Linked accounts" row 탭 시 열린다.
 *
 * 현재 계정에 연결된 로그인 수단 목록 + 아직 연결 안 된 provider 를 연결(link)/해제(unlink) 한다.
 * 링크 시 그 identity 가 다른 계정 소속이면 BFF 가 계정을 병합하고 이월 크레딧을 안내한다.
 * Apple 은 iOS 에서만 노출 — Android 엔 Apple 네이티브 SDK 가 없다([LoginScreen] 과 동일 정책).
 *
 * 목록·잔액·상태는 모두 [UserMenuViewModel] 소유이므로 부모 시트와 같은 VM 인스턴스를 넘겨받는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedAccountsSheet(
    viewModel: UserMenuViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by viewModel.link.collectAsState()
    val tokens = LocalVibiColors.current
    var confirmUnlink by remember { mutableStateOf<IdentityProvider?>(null) }

    LaunchedEffect(viewModel) {
        // 직전에 열렸을 때 남은 배너를 리셋 — message 는 오래 사는 VM 소유라 안 지우면 재오픈 시 재등장.
        viewModel.clearLinkMessage()
        viewModel.loadIdentities()
    }

    val identities = state.identities
    val linkedProviders = identities.orEmpty().map { it.provider }.toSet()
    val busy = state.busyProvider != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.panelBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Linked accounts",
                style = TextStyle(
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = "Sign in with any linked account. Linking an account that already exists " +
                    "merges its credits into this one.",
                style = TextStyle(fontSize = 13.sp, color = tokens.mutedText),
            )

            state.message?.let { MessageBanner(it) }

            when {
                // 최초 조회 실패 — 무한 스피너 대신 오류 + 재시도.
                identities == null && state.loadFailed -> LoadFailedRow(onRetry = viewModel::loadIdentities)

                // 최초 로딩(아직 목록 없음) — 스피너.
                identities == null -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = tokens.accent,
                    )
                }

                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    identities.forEach { identity ->
                        IdentityRow(
                            identity = identity,
                            // 로그인 수단이 하나뿐이면 해제 불가(BFF 409) — 버튼 자체를 감춘다.
                            canUnlink = identities.size > 1,
                            busy = state.busyProvider == identity.provider,
                            onUnlink = { confirmUnlink = identity.provider },
                        )
                    }
                }
            }

            // 아직 연결 안 된 provider 만 링크 버튼 노출.
            val canLinkGoogle = IdentityProvider.GOOGLE !in linkedProviders
            val canLinkApple = isIosPlatform && IdentityProvider.APPLE !in linkedProviders
            if (canLinkGoogle || canLinkApple) {
                Text(
                    text = "Link another account",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = tokens.mutedText,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                if (canLinkGoogle) {
                    GoogleSignInButton(
                        onClick = viewModel::linkGoogle,
                        enabled = !busy,
                        loading = state.busyProvider == IdentityProvider.GOOGLE,
                        label = "Link Google account",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (canLinkApple) {
                    AppleSignInButton(
                        onClick = viewModel::linkApple,
                        enabled = !busy,
                        loading = state.busyProvider == IdentityProvider.APPLE,
                        label = "Link Apple account",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    confirmUnlink?.let { provider ->
        val typo = LocalVibiTypography.current
        VibiDialog(
            title = "Unlink ${provider.displayName}?",
            onDismiss = { confirmUnlink = null },
            primary = {
                VibiPrimaryButton(
                    "Unlink",
                    destructive = true,
                    onClick = {
                        confirmUnlink = null
                        viewModel.unlink(provider)
                    },
                )
            },
        ) {
            Text(
                "You'll no longer be able to sign in with your ${provider.displayName} account. " +
                    "Your account, credits, and projects stay the same.",
                style = typo.bodySm,
                color = tokens.mutedText,
            )
        }
    }
}

@Composable
private fun IdentityRow(
    identity: LinkedIdentity,
    canUnlink: Boolean,
    busy: Boolean,
    onUnlink: () -> Unit,
) {
    val tokens = LocalVibiColors.current
    VibiChipRow(verticalPadding = 12.dp) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = identity.provider.displayName,
                    style = TextStyle(
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                if (identity.primary) {
                    Spacer(Modifier.width(8.dp))
                    PrimaryBadge()
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = identity.email,
                style = TextStyle(fontSize = 12.sp, color = tokens.mutedText),
                maxLines = 1,
            )
        }
        when {
            busy -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = tokens.accent,
            )
            canUnlink -> Text(
                text = "Unlink",
                style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onUnlink)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun PrimaryBadge() {
    val tokens = LocalVibiColors.current
    Text(
        text = "PRIMARY",
        style = TextStyle(
            fontSize = 9.sp,
            color = tokens.mutedText,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tokens.chipBgDisabled)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** 목록 조회 실패 시: 오류 문구 + 탭하면 재조회하는 "Retry". 무한 스피너를 대체한다. */
@Composable
private fun LoadFailedRow(onRetry: () -> Unit) {
    val tokens = LocalVibiColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Couldn't load linked accounts",
            style = TextStyle(fontSize = 13.sp, color = tokens.mutedText),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Retry",
            style = TextStyle(
                fontSize = 14.sp,
                color = tokens.accent,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRetry)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun MessageBanner(message: LinkMessage) {
    val tokens = LocalVibiColors.current
    val (text, isError) = message.resolve()
    Text(
        text = text,
        style = TextStyle(
            fontSize = 13.sp,
            color = if (isError) MaterialTheme.colorScheme.error else tokens.accent,
            fontWeight = FontWeight.Medium,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

private val IdentityProvider.displayName: String
    get() = when (this) {
        IdentityProvider.GOOGLE -> "Google"
        IdentityProvider.APPLE -> "Apple"
    }

/** LinkMessage → (표시 문구, 에러 여부). 에러면 error 색, 아니면 accent 색으로 표시. */
private fun LinkMessage.resolve(): Pair<String, Boolean> = when (this) {
    is LinkMessage.Merged ->
        (if (credits > 0) "Accounts merged — $credits credits added" else "Accounts merged") to false
    LinkMessage.Linked -> "Account linked" to false
    LinkMessage.AlreadyLinked -> "That account is already linked here" to false
    LinkMessage.Unlinked -> "Account unlinked" to false
    LinkMessage.ProviderConflict -> "That account is already linked elsewhere" to true
    LinkMessage.CannotUnlinkLast -> "Keep at least one way to sign in" to true
    LinkMessage.NotLinked -> "That account isn't linked" to true
    LinkMessage.TimedOut -> "Timed out — please try again" to true
    LinkMessage.Error -> "Something went wrong — please try again" to true
}
