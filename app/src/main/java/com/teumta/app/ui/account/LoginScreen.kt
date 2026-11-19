package com.teumta.app.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teumta.core.designsystem.component.TeumtaAppBar
import com.teumta.core.designsystem.component.TeumtaButton
import com.teumta.core.designsystem.component.TeumtaTextField
import com.teumta.core.designsystem.icon.TeumtaIcons
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaTheme

@Composable
fun LoginRoute(
    onBack: () -> Unit,
    onDone: () -> Unit,
    onSignupClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 로그인에 성공하면 한 번만 이전 화면으로 돌아간다
    LaunchedEffect(state.done) { if (state.done) onDone() }
    LoginScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::onSubmit,
        onBack = onBack,
        onSignupClick = onSignupClick,
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onSignupClick: () -> Unit,
) {
    Scaffold(topBar = { TeumtaAppBar(title = "로그인", onBack = onBack, modifier = Modifier.statusBarsPadding()) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding() // 키보드가 올라와도 버튼이 가려지지 않게
                .verticalScroll(rememberScrollState())
                .padding(TeumtaDimens.Space20),
            verticalArrangement = Arrangement.spacedBy(TeumtaDimens.Space12),
        ) {
            Text(
                "로그인하면 집역·회사역을 저장해서 다른 휴대폰에서도 그대로 쓸 수 있어요.",
                style = TeumtaTheme.typography.body,
                color = TeumtaTheme.colors.textSecondary,
            )
            TeumtaTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "이메일",
                leadingIcon = TeumtaIcons.Mail,
                errorText = state.emailError,
                enabled = !state.isSubmitting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, hintLocales = EnglishKeyboard),
            )
            TeumtaTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = "비밀번호",
                leadingIcon = TeumtaIcons.Lock,
                errorText = state.passwordError,
                enabled = !state.isSubmitting,
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, hintLocales = EnglishKeyboard),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            )
            ServerError(state.serverError)
            TeumtaButton(
                text = if (state.isSubmitting) "로그인하는 중…" else "로그인",
                onClick = onSubmit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onSignupClick, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("계정이 없나요? 회원가입")
            }
        }
    }
}

// 서버가 거절한 이유. 화면 읽기(TalkBack)가 나타나자마자 읽어 준다
@Composable
internal fun ServerError(message: String?) {
    if (message == null) return
    Text(
        message,
        style = TeumtaTheme.typography.bodyS,
        color = TeumtaTheme.colors.textDanger,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    TeumtaTheme {
        LoginScreen(
            state = LoginUiState(email = "me@example.com", serverError = "이메일 또는 비밀번호가 맞지 않아요."),
            onEmailChange = {}, onPasswordChange = {}, onSubmit = {}, onBack = {}, onSignupClick = {},
        )
    }
}
