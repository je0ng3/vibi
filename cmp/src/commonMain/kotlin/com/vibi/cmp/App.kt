package com.vibi.cmp

import androidx.compose.runtime.Composable
import com.vibi.cmp.theme.VibiTheme
import com.vibi.cmp.ui.navigation.VibiNavHost

@Composable
fun App() {
    // 시스템 다크/라이트 모드 자동 감지.
    VibiTheme {
        VibiNavHost()
    }
}
