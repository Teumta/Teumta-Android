package com.teumta.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.teumta.app.ui.account.CommuteRoute
import com.teumta.app.ui.account.LoginRoute
import com.teumta.app.ui.account.SettingsRoute
import com.teumta.app.ui.account.SignupRoute
import com.teumta.app.ui.info.InfoScreen
import com.teumta.app.ui.input.InputOpenRequest
import com.teumta.app.ui.input.InputRoute
import com.teumta.app.ui.input.InputViewModel
import com.teumta.app.ui.result.ResultActions
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
        composable<ResultDestination> { entry ->
            // 뒤로 가면 입력 화면의 ViewModel이 그대로라서 고른 값이 남아 있다.
            // "대신 이렇게 해 보세요"는 그 ViewModel의 값을 바꾸고 돌아간다(입력 화면 기록에서 같은 것을 받아 온다)
            val inputEntry = remember(entry) { navController.getBackStackEntry<InputDestination>() }
            val inputViewModel: InputViewModel = hiltViewModel(inputEntry)
            ResultRoute(
                actions = ResultActions(
                    onBack = { navController.popBackStack() },
                    onReduceStay = { minutes ->
                        inputViewModel.onStayChange(minutes)
                        navController.popBackStack()
                    },
                    onGoDirect = {
                        inputViewModel.onClearVia()
                        inputViewModel.onCalculate()?.let { input ->
                            navController.popBackStack()
                            navController.navigate(input.toDestination())
                        }
                    },
                    onChangeArrive = {
                        inputViewModel.requestOpen(InputOpenRequest.ARRIVE_SHEET)
                        navController.popBackStack()
                    },
                    onAddVia = {
                        inputViewModel.requestOpen(InputOpenRequest.VIA_PICKER)
                        navController.popBackStack()
                    },
                ),
            )
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
