package com.teumta.app.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teumta.core.designsystem.component.TeumtaAppBar
import com.teumta.core.designsystem.component.TeumtaButton
import com.teumta.core.designsystem.component.TeumtaButtonStyle
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaTheme

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit,
    onInfoClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBack = onBack,
        onLoginClick = onLoginClick,
        onSignupClick = onSignupClick,
        onLogout = viewModel::onLogout,
        onWithdraw = viewModel::onWithdraw,
        onInfoClick = onInfoClick,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit,
    onLogout: () -> Unit,
    onWithdraw: () -> Unit,
    onInfoClick: () -> Unit,
) {
    // 탈퇴는 되돌릴 수 없어서 한 번 더 묻는다. 대화상자를 열었는지는 화면만 알면 된다
    var confirmWithdraw by rememberSaveable { mutableStateOf(false) }
    val colors = TeumtaTheme.colors
    val type = TeumtaTheme.typography

    Scaffold(topBar = { TeumtaAppBar(title = "설정", onBack = onBack, modifier = Modifier.statusBarsPadding()) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(TeumtaDimens.Space20),
            verticalArrangement = Arrangement.spacedBy(TeumtaDimens.Space16),
        ) {
            Section("계정") {
                if (state.isLoggedIn) {
                    Text(state.email ?: "불러오는 중…", style = type.bodyStrong, color = colors.textPrimary)
                    TeumtaButton(
                        text = "로그아웃",
                        onClick = onLogout,
                        style = TeumtaButtonStyle.Outlined,
                        enabled = !state.isWorking,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = { confirmWithdraw = true }, enabled = !state.isWorking) {
                        Text("회원 탈퇴", color = colors.textDanger)
                    }
                } else {
                    Text(
                        "로그인하면 집역·회사역을 저장해서 다른 휴대폰에서도 그대로 쓸 수 있어요. 로그인하지 않아도 열차 계산은 쓸 수 있어요.",
                        style = type.body,
                        color = colors.textSecondary,
                    )
                    TeumtaButton(text = "로그인", onClick = onLoginClick, modifier = Modifier.fillMaxWidth())
                    TeumtaButton(
                        text = "회원가입",
                        onClick = onSignupClick,
                        style = TeumtaButtonStyle.Outlined,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                state.message?.let {
                    Text(it, style = type.bodyS, color = colors.textSecondary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }

            Section("앱") {
                TextButton(onClick = onInfoClick) { Text("앱 정보 · 개인정보처리방침 · 오픈소스") }
            }
        }
    }

    if (confirmWithdraw) {
        AlertDialog(
            onDismissRequest = { confirmWithdraw = false },
            title = { Text("탈퇴할까요?") },
            text = { Text("계정과 저장한 집역·회사역이 서버에서 바로 지워지고 되돌릴 수 없어요.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmWithdraw = false
                    onWithdraw()
                }) { Text("탈퇴", color = colors.textDanger) }
            },
            dismissButton = { TextButton(onClick = { confirmWithdraw = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(TeumtaDimens.Space16), verticalArrangement = Arrangement.spacedBy(TeumtaDimens.Space8)) {
            Text(title, style = TeumtaTheme.typography.titleS, color = TeumtaTheme.colors.textPrimary)
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsLoggedOutPreview() {
    TeumtaTheme {
        SettingsScreen(SettingsUiState(), onBack = {}, onLoginClick = {}, onSignupClick = {}, onLogout = {}, onWithdraw = {}, onInfoClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsLoggedInPreview() {
    TeumtaTheme {
        SettingsScreen(
            SettingsUiState(isLoggedIn = true, email = "me@example.com"),
            onBack = {}, onLoginClick = {}, onSignupClick = {}, onLogout = {}, onWithdraw = {}, onInfoClick = {},
        )
    }
}
