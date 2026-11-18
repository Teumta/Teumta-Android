package com.teumta.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.teumta.app.ui.navigation.TeumtaNavHost
import com.teumta.core.designsystem.theme.TeumtaTheme
import dagger.hilt.android.AndroidEntryPoint

// 이 화면 안에서 hiltViewModel()로 ViewModel를 받을 수 있게 한다
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TeumtaTheme {
                TeumtaNavHost()
            }
        }
    }
}
