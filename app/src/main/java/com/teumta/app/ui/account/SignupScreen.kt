package com.teumta.app.ui.account

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teumta.app.ui.info.AppInfo
import com.teumta.core.designsystem.component.TeumtaAppBar
import com.teumta.core.designsystem.component.TeumtaHelpButton
import com.teumta.core.designsystem.component.TeumtaButton
import com.teumta.core.designsystem.component.TeumtaTextField
import com.teumta.core.designsystem.icon.TeumtaIcons
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaTheme

@Composable
fun SignupRoute(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    SignupScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onAdultChange = viewModel::onAdultChange,
        onSubmit = viewModel::onSubmit,
        onBack = onBack,
    )
}

@Composable
fun SignupScreen(
    state: SignupUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAdultChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Scaffold(topBar = { TeumtaAppBar(title = "회원가입", onBack = onBack, modifier = Modifier.statusBarsPadding(), actions = { TeumtaHelpButton("가입하면", SIGNUP_HELP) }) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(TeumtaDimens.Space20),
            verticalArrangement = Arrangement.spacedBy(TeumtaDimens.Space12),
        ) {
            TeumtaTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "이메일",
                leadingIcon = TeumtaIcons.Mail,
                errorText = state.emailError,
                helperText = "로그인할 때 써요. 인증 메일은 보내지 않아요.",
                enabled = !state.isSubmitting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, hintLocales = EnglishKeyboard),
            )
            TeumtaTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = "비밀번호",
                leadingIcon = TeumtaIcons.Lock,
                errorText = state.passwordError,
                helperText = "영문·숫자·기호 8~64자",
                enabled = !state.isSubmitting,
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, hintLocales = EnglishKeyboard),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            )
            // 줄 전체를 눌러도 체크되고, TalkBack은 "체크박스, 만 14세 이상이에요"로 읽는다
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TeumtaDimens.SizeTouchMin)
                    .toggleable(value = state.isAdult, onValueChange = onAdultChange, role = Role.Checkbox, enabled = !state.isSubmitting),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = state.isAdult, onCheckedChange = null)
                Text("만 14세 이상이에요", style = TeumtaTheme.typography.body, modifier = Modifier.padding(start = TeumtaDimens.Space8))
            }
            state.ageError?.let { Text(it, style = TeumtaTheme.typography.bodyS, color = TeumtaTheme.colors.textDanger) }
            TextButton(onClick = {
                // 브라우저가 없으면 openUri가 예외를 던져 앱이 꺼진다(앱 정보 화면과 같은 처리)
                runCatching { uriHandler.openUri(AppInfo.PRIVACY_URL) }
                    .onFailure { Toast.makeText(context, "열 수 있는 앱이 없어요: ${AppInfo.PRIVACY_URL}", Toast.LENGTH_LONG).show() }
            }) { Text("개인정보처리방침 보기") }
            ServerError(state.serverError)
            TeumtaButton(
                text = if (state.isSubmitting) "가입하는 중…" else "가입하기",
                onClick = onSubmit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// 가입하면 무엇을 어디에 저장하는지. 자세한 내용은 개인정보처리방침 보기
private val SIGNUP_HELP = listOf(
    "이메일과 비밀번호를 틈타 서버(호주)에 저장해요.",
    "비밀번호는 알아볼 수 없는 값으로 바꿔서 저장해요.",
    "앱 설정에서 언제든 탈퇴할 수 있고, 탈퇴하면 바로 지워요.",
)

@Preview(showBackground = true)
@Composable
private fun SignupScreenPreview() {
    TeumtaTheme {
        SignupScreen(
            state = SignupUiState(email = "me", emailError = "이메일 형식이 아니에요."),
            onEmailChange = {}, onPasswordChange = {}, onAdultChange = {}, onSubmit = {}, onBack = {},
        )
    }
}
