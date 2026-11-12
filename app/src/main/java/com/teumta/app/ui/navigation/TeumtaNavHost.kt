package com.teumta.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.teumta.app.ui.input.InputRoute
import com.teumta.app.ui.result.ResultRoute

@Composable
fun TeumtaNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = InputDestination) {
        composable<InputDestination> {
            InputRoute(onCalculate = { input -> navController.navigate(input.toDestination()) })
        }
        composable<ResultDestination> {
            // 뒤로 가면 입력 화면의 ViewModel이 그대로라서 고른 값이 남아 있다
            ResultRoute(onBack = { navController.popBackStack() })
        }
    }
}
