package com.teumta.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.teumta.app.ui.account.CommuteRoute
import com.teumta.app.ui.account.LoginRoute
import com.teumta.app.ui.account.SettingsRoute
import com.teumta.app.ui.account.SignupRoute
import com.teumta.app.ui.info.InfoScreen
import com.teumta.app.ui.input.InputRoute
import com.teumta.app.ui.result.ResultRoute

@Composable
fun TeumtaNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = InputDestination) {
        composable<InputDestination> {
            InputRoute(
                onCalculate = { input -> navController.navigate(input.toDestination()) },
                onSettingsClick = { navController.navigate(SettingsDestination) },
            )
        }
        composable<ResultDestination> {
            // 뒤로 가면 입력 화면의 ViewModel이 그대로라서 고른 값이 남아 있다
            ResultRoute(onBack = { navController.popBackStack() })
        }
        composable<InfoDestination> {
            InfoScreen(onBack = { navController.popBackStack() })
        }
        composable<SettingsDestination> {
            SettingsRoute(
                onBack = { navController.popBackStack() },
                onLoginClick = { navController.navigate(LoginDestination) },
                onSignupClick = { navController.navigate(SignupDestination) },
                onInfoClick = { navController.navigate(InfoDestination) },
                onCommuteClick = { navController.navigate(CommuteDestination) },
            )
        }
        composable<CommuteDestination> {
            CommuteRoute(
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
        composable<LoginDestination> {
            LoginRoute(
                onBack = { navController.popBackStack() },
                // 로그인·가입에 성공하면 설정 화면까지 돌아간다(로그인 → 회원가입으로 왔어도 둘 다 닫는다)
                onDone = { navController.popBackStack(SettingsDestination, inclusive = false) },
                onSignupClick = { navController.navigate(SignupDestination) },
            )
        }
        composable<SignupDestination> {
            SignupRoute(
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack(SettingsDestination, inclusive = false) },
            )
        }
    }
}
