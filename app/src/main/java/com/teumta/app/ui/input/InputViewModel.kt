package com.teumta.app.ui.input

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teumta.app.data.LastRouteStore
import com.teumta.app.data.PolicyNoticeStore
import com.teumta.app.data.TimetableRepository
import com.teumta.app.data.account.AccountRepository
import com.teumta.app.domain.ServiceTime
import com.teumta.app.domain.StationTree
import com.teumta.app.domain.model.LastRoute
import com.teumta.app.domain.model.Station
import com.teumta.app.ui.info.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

// 입력 화면의 상태를 들고 있다. 화면 회전에는 ViewModel이, 앱을 다시 열 때는 LastRouteStore(DataStore)가 고른 값을 지킨다
@HiltViewModel
class InputViewModel @Inject constructor(
    private val repository: TimetableRepository,
    private val lastRouteStore: LastRouteStore,
    private val accountRepository: AccountRepository,
    private val policyNoticeStore: PolicyNoticeStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InputUiState(arriveBy = defaultArriveBy(ServiceTime.now())))
    val uiState: StateFlow<InputUiState> = _uiState.asStateFlow()

    init {
        // 계산 버튼을 눌렀을 때 기다리지 않게, 역을 고르는 동안 열차 시간표를 미리 읽어 둔다
        viewModelScope.launch { repository.getTimetable() }
        viewModelScope.launch {
            val seen = policyNoticeStore.seenVersion.first()
            if (seen != AppInfo.PRIVACY_VERSION) _uiState.update { it.copy(showPolicyNotice = true) }
        }
        viewModelScope.launch {
            accountRepository.isLoggedIn.collect { loggedIn -> if (loggedIn) loadCommute() }
        }
        viewModelScope.launch {
            val tree = StationTree(repository.getStations())
            // 지난번에 계산한 경로가 있으면 그 값으로 채우고, 없거나 쓸 수 없으면 예시 경로로 채운다
            val saved = lastRouteStore.lastRoute.first()
            val selection = saved?.let { restoreSelection(tree, it) } ?: exampleSelection(tree)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    sections = stationSections(tree),
                    selection = selection,
                    stayMin = saved?.stayMin?.takeIf(StayChoice::isValid) ?: it.stayMin,
                )
            }
            // 자주 쓰는 두 역이 바뀔 때마다(설정에서 저장, 로그아웃) 역 ID를 역으로 바꿔 둔다
            launch {
                accountRepository.commute.collect { commute ->
                    _uiState.update {
                        it.copy(
                            home = commute?.homeStationId?.let(tree::find),
                            work = commute?.workStationId?.let(tree::find),
                        )
                    }
                }
            }
        }
    }

    // 로그인해 있으면 서버에서 자주 쓰는 두 역을 받아 둔다(commute가 바뀐다).
    // 인터넷이 없어도 계산에는 지장이 없어서, 실패하면 버튼만 안 보이고 넘어간다
    private suspend fun loadCommute() {
        try {
            accountRepository.getMe()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 버튼을 보여 주지 않는다
        }
    }

    // 안내를 닫거나 방침을 열면 이 판은 다시 보여 주지 않는다
    fun onPolicyNoticeSeen() {
        _uiState.update { it.copy(showPolicyNotice = false) }
        viewModelScope.launch { policyNoticeStore.markSeen(AppInfo.PRIVACY_VERSION) }
    }

    fun onCommute(direction: CommuteDirection) {
        _uiState.update { state -> state.commuteSelection(direction)?.let { state.copy(selection = it) } ?: state }
    }

    // 역 고르기 화면에서 고른 역을 그 칸에 넣는다
    fun onStationPick(role: StationRole, station: Station) {
        _uiState.update { it.copy(selection = it.selection.pick(role, station)) }
    }

    fun onClearVia() {
        _uiState.update { it.copy(selection = it.selection.withoutVia()) }
    }

    fun onSwap() {
        _uiState.update { it.copy(selection = it.selection.swapped()) }
    }

    fun onArriveByChange(time: LocalTime) {
        _uiState.update { it.copy(arriveBy = time) }
    }

    fun onStayChange(minutes: Int) {
        _uiState.update { it.copy(stayMin = minutes) }
    }

    // 계산 버튼: 지금 고른 경로를 저장하고, 결과 화면으로 넘길 값을 돌려준다
    fun onCalculate(): RouteInput? {
        val input = _uiState.value.toRouteInput() ?: return null
        // 파일 저장은 시간이 걸려서 기다리지 않고 바로 화면을 넘긴다
        viewModelScope.launch {
            lastRouteStore.save(LastRoute(input.fromId, input.viaId, input.toId, input.stayMin))
        }
        return input
    }

    // 처음 연 사람(심사자 포함)이 바로 계산해 볼 수 있게 예시 경로를 채워 둔다
    private fun exampleSelection(tree: StationTree): RouteSelection {
        val byName = tree.stations.associateBy { it.name }
        return RouteSelection(tree, from = byName["서울역"], to = byName["인천"], via = byName["구로"])
    }
}
