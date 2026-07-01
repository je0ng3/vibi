package com.vibi.cmp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 35+ 는 edge-to-edge 강제 — 명시 호출로 SystemBarStyle.auto 를 적용해
        // 상태바/내비바 아이콘 명암을 시스템 다크/라이트에 맞춰 자동 반전(iOS 동등 가독성).
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}
