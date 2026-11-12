package com.teumta.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.teumta.app.ui.input.InputRoute
import com.teumta.app.ui.theme.TeumtaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TeumtaTheme {
                // 결과 화면은 다음 이슈에서 붙인다
                InputRoute(onCalculate = {})
            }
        }
    }
}
