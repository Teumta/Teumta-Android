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

data class SignupUiState(
    val email: String = "",
    val password: String = "",
    // 비밀번호 확인. 서버로 보내지 않고 앱에서 두 칸이 같은지만 본다
    val passwordConfirm: String = "",
    // 만 14세 이상인지. 개인정보처리방침에 "만 14세 미만은 가입할 수 없어요"라고 적었다
    val isAdult: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val passwordConfirmError: String? = null,
    val ageError: String? = null,
    val serverError: String? = null,
    val isSubmitting: Boolean = false,
    val done: Boolean = false,
)

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val repository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email, emailError = null, serverError = null) }

    // 비밀번호를 고치면 확인 칸의 "서로 달라요"도 다시 맞춰 볼 때까지 지운다
    fun onPasswordChange(password: String) =
        _uiState.update { it.copy(password = password, passwordError = null, passwordConfirmError = null, serverError = null) }

    fun onPasswordConfirmChange(confirm: String) =
        _uiState.update { it.copy(passwordConfirm = confirm, passwordConfirmError = null, serverError = null) }

    fun onAdultChange(isAdult: Boolean) = _uiState.update { it.copy(isAdult = isAdult, ageError = null) }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val emailError = AccountRules.emailError(state.email)
        val passwordError = AccountRules.passwordError(state.password)
        val passwordConfirmError = AccountRules.passwordConfirmError(state.password, state.passwordConfirm)
        val ageError = if (state.isAdult) null else "만 14세 이상만 가입할 수 있어요."
        if (emailError != null || passwordError != null || passwordConfirmError != null || ageError != null) {
            _uiState.update {
                it.copy(emailError = emailError, passwordError = passwordError, passwordConfirmError = passwordConfirmError, ageError = ageError)
            }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, serverError = null) }
        viewModelScope.launch {
            try {
                repository.signup(state.email, state.password)
                _uiState.update { it.copy(isSubmitting = false, done = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, serverError = e.toUserMessage()) }
            }
        }
    }
}
