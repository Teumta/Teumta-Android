package com.teumta.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teumta.app.data.TimetableRepository
import com.teumta.app.data.account.AccountRepository
import com.teumta.app.data.account.toUserMessage
import com.teumta.app.domain.StationTree
import com.teumta.app.domain.model.Station
import com.teumta.app.ui.input.StationSection
import com.teumta.app.ui.input.stationSections
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 지금 어느 역을 고르는 중인지
enum class CommuteSlot { HOME, WORK }

data class CommuteUiState(
    val isLoading: Boolean = true,
    // 역 목록(노선도를 갈림길에서 끊은 토막). 입력 화면과 같은 순서로 보여 준다
    val sections: List<StationSection> = emptyList(),
    val home: Station? = null,
    val work: Station? = null,
    // null이 아니면 역 목록을 펼쳐 그 칸의 역을 고르는 중
    val picking: CommuteSlot? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    // 저장에 성공했다. 화면이 보고 설정으로 돌아간다
    val done: Boolean = false,
)

@HiltViewModel
class CommuteViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommuteUiState())
    val uiState: StateFlow<CommuteUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 역 목록은 앱 안 시간표에서 바로 읽는다(서버를 기다리지 않는다)
            val tree = StationTree(timetableRepository.getStations())
            // 아직 서버에서 받은 적이 없으면 한 번 받는다. 실패하면 빈 칸에서 시작한다
            val commute = accountRepository.commute.value ?: try {
                accountRepository.getMe()
                accountRepository.commute.value
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    sections = stationSections(tree),
                    home = commute?.homeStationId?.let(tree::find),
                    work = commute?.workStationId?.let(tree::find),
                )
            }
        }
    }

    fun onPick(slot: CommuteSlot) = _uiState.update { it.copy(picking = slot, error = null) }

    fun onCancelPick() = _uiState.update { it.copy(picking = null) }

    fun onStationPicked(station: Station) = _uiState.update {
        when (it.picking) {
            CommuteSlot.HOME -> it.copy(home = station, picking = null)
            CommuteSlot.WORK -> it.copy(work = station, picking = null)
            null -> it
        }
    }

    fun onClear(slot: CommuteSlot) = _uiState.update {
        when (slot) {
            CommuteSlot.HOME -> it.copy(home = null, error = null)
            CommuteSlot.WORK -> it.copy(work = null, error = null)
        }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving) return
        // 서버도 같은 규칙(SAME_HOME_AND_WORK)으로 막지만, 앱에서 먼저 알려 준다
        if (state.home != null && state.home == state.work) {
            _uiState.update { it.copy(error = "집역과 회사역을 다르게 골라 주세요.") }
            return
        }
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                accountRepository.updateStations(state.home?.id, state.work?.id)
                _uiState.update { it.copy(isSaving = false, done = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.toUserMessage()) }
            }
        }
    }
}
