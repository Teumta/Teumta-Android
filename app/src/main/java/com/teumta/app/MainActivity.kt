package com.teumta.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.teumta.app.ui.navigation.TeumtaNavHost
import com.teumta.core.designsystem.theme.TeumtaTheme
import dagger.hilt.android.AndroidEntryPoint

// 이 화면 안에서 hiltViewModel()로 ViewModel를 받을 수 있게 한다
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 매니페스트의 시작 테마(Theme.Teumta.Starting)로 스플래시를 보여 주고, 첫 화면이 그려지면 앱 테마로 바꾼다.
        // super.onCreate보다 먼저 불러야 한다. 아래 '틈타' 글자는 Android 12부터만 나온다(values-v31)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TeumtaTheme {
                TeumtaNavHost()
            }
        }
    }
}
