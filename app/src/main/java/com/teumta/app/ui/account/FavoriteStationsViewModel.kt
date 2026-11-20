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

// 지금 어느 칸(출발역, 도착역)을 고르는 중인지
enum class FavoriteSlot { ORIGIN, DESTINATION }

data class FavoriteStationsUiState(
    val isLoading: Boolean = true,
    // 역 목록(노선도를 갈림길에서 끊은 토막). 입력 화면과 같은 순서로 보여 준다
    val sections: List<StationSection> = emptyList(),
    val origin: Station? = null,
    val destination: Station? = null,
    // null이 아니면 역 목록을 펼쳐 그 칸의 역을 고르는 중
    val picking: FavoriteSlot? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    // 저장에 성공했다. 화면이 보고 설정으로 돌아간다
    val done: Boolean = false,
)

@HiltViewModel
class FavoriteStationsViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoriteStationsUiState())
    val uiState: StateFlow<FavoriteStationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 역 목록은 앱 안 시간표에서 바로 읽는다(서버를 기다리지 않는다)
            val tree = StationTree(timetableRepository.getStations())
            // 아직 서버에서 받은 적이 없으면 한 번 받는다. 실패하면 빈 칸에서 시작한다
            val favoriteStations = accountRepository.favoriteStations.value ?: try {
                accountRepository.getMe()
                accountRepository.favoriteStations.value
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    sections = stationSections(tree),
                    origin = favoriteStations?.originStationId?.let(tree::find),
                    destination = favoriteStations?.destinationStationId?.let(tree::find),
                )
            }
        }
    }

    fun onPick(slot: FavoriteSlot) = _uiState.update { it.copy(picking = slot, error = null) }

    fun onCancelPick() = _uiState.update { it.copy(picking = null) }

    fun onStationPicked(station: Station) = _uiState.update {
        when (it.picking) {
            FavoriteSlot.ORIGIN -> it.copy(origin = station, picking = null)
            FavoriteSlot.DESTINATION -> it.copy(destination = station, picking = null)
            null -> it
        }
    }

    fun onClear(slot: FavoriteSlot) = _uiState.update {
        when (slot) {
            FavoriteSlot.ORIGIN -> it.copy(origin = null, error = null)
            FavoriteSlot.DESTINATION -> it.copy(destination = null, error = null)
        }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving) return
        // 서버도 같은 규칙(SAME_STATIONS)으로 막지만, 앱에서 먼저 알려 준다
        if (state.origin != null && state.origin == state.destination) {
            _uiState.update { it.copy(error = "출발역과 도착역을 다르게 골라 주세요.") }
            return
        }
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                accountRepository.updateStations(state.origin?.id, state.destination?.id)
                _uiState.update { it.copy(isSaving = false, done = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.toUserMessage()) }
            }
        }
    }
}
