package com.teumta.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teumta.app.data.TimetableRepository
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

data class SettingsUiState(
    val isLoggedIn: Boolean = false,
    // 서버에서 받은 내 이메일. 아직 못 받았거나 못 받으면 null
    val email: String? = null,
    // 저장한 집역·회사역 이름. 정하지 않았으면 null
    val homeName: String? = null,
    val workName: String? = null,
    // 로그아웃·탈퇴를 보내는 중이면 버튼을 막는다
    val isWorking: Boolean = false,
    // 화면 아래에 보여 줄 결과나 오류 ("로그아웃했어요", "서버에 연결하지 못했어요" 등)
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AccountRepository,
    private val timetableRepository: TimetableRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // 집역·회사역이 바뀌면(저장, 로그아웃) 역 이름으로 바꿔 보여 준다
        viewModelScope.launch {
            val names = timetableRepository.getStations().associate { it.id to it.name }
            repository.commute.collect { commute ->
                _uiState.update {
                    it.copy(homeName = commute?.homeStationId?.let(names::get), workName = commute?.workStationId?.let(names::get))
                }
            }
        }
        // 로그인 상태가 바뀔 때마다(로그인 화면에서 돌아옴, 로그아웃, 갱신 실패) 화면을 맞춘다
        viewModelScope.launch {
            repository.isLoggedIn.collect { loggedIn ->
                _uiState.update {
                    if (loggedIn) {
                        // 로그인하고 돌아왔으면 지난 안내("로그아웃했어요")는 지운다
                        it.copy(isLoggedIn = true, message = null)
                    } else {
                        // 안내는 남긴다. 로그아웃·탈퇴 결과가 이 변화보다 먼저 적힐 수 있다
                        it.copy(isLoggedIn = false, email = null)
                    }
                }
                if (loggedIn) loadMe()
            }
        }
    }

    private suspend fun loadMe() {
        try {
            val me = repository.getMe()
            _uiState.update { it.copy(email = me.email) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 로그인이 풀렸으면 isLoggedIn이 false로 바뀌고, 이유는 여기서 보여 준다
            _uiState.update { it.copy(message = e.toUserMessage()) }
        }
    }

    fun onLogout() = run("로그아웃했어요.") { repository.logout() }

    fun onWithdraw() = run("탈퇴했어요. 계정과 저장한 정보를 지웠어요.") { repository.withdraw() }

    private fun run(doneMessage: String, action: suspend () -> Unit) {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true, message = null) }
        viewModelScope.launch {
            val message = try {
                action()
                doneMessage
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.toUserMessage()
            }
            _uiState.update { it.copy(isWorking = false, message = message) }
        }
    }
}
