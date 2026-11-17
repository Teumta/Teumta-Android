package com.teumta.app.ui.input

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teumta.app.data.LastRouteStore
import com.teumta.app.data.TimetableRepository
import com.teumta.app.domain.ServiceTime
import com.teumta.app.domain.StationTree
import com.teumta.app.domain.model.LastRoute
import com.teumta.app.domain.model.Station
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(InputUiState(arriveBy = defaultArriveBy(ServiceTime.now())))
    val uiState: StateFlow<InputUiState> = _uiState.asStateFlow()

    init {
        // 계산 버튼을 눌렀을 때 기다리지 않게, 역을 고르는 동안 열차 시간표를 미리 읽어 둔다
        viewModelScope.launch { repository.getTimetable() }
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
                    stayMin = saved?.stayMin?.takeIf { min -> min in STAY_OPTIONS } ?: it.stayMin,
                )
            }
        }
    }

    fun onStationTap(station: Station) {
        _uiState.update { it.copy(selection = it.selection.tap(station)) }
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

    companion object {
        val STAY_OPTIONS = listOf(10, 30, 60)
    }
}
