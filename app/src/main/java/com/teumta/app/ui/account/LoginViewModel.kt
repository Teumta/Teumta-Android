package com.teumta.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teumta.app.data.account.AccountRepository
import com.teumta.app.data.account.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    // 서버가 거절한 이유(비밀번호 틀림, 인터넷 없음 등)
    val serverError: String? = null,
    val isSubmitting: Boolean = false,
    // 로그인에 성공했다. 화면이 보고 이전 화면으로 돌아간다
    val done: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // 고치기 시작하면 그 칸의 오류는 지운다
    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email, emailError = null, serverError = null) }

    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password, passwordError = null, serverError = null) }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return // 두 번 눌러도 한 번만 보낸다
        val emailError = AccountRules.loginEmailError(state.email)
        val passwordError = AccountRules.loginPasswordError(state.password)
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, serverError = null) }
        viewModelScope.launch {
            try {
                repository.login(state.email, state.password)
                _uiState.update { it.copy(isSubmitting = false, done = true) }
            } catch (e: CancellationException) {
                throw e // 화면을 떠나 취소된 것은 오류가 아니다
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, serverError = e.toUserMessage()) }
            }
        }
    }
}
