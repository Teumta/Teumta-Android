package com.teumta.app.ui.input

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.teumta.app.TeumtaApplication
import com.teumta.app.data.TimetableRepository
import com.teumta.app.domain.ServiceTime
import com.teumta.app.domain.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

// 입력 화면의 상태를 들고 있다. 화면을 돌려도(회전) ViewModel은 남아 있어서 고른 값이 유지된다
class InputViewModel(private val repository: TimetableRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(InputUiState(arriveBy = defaultArriveBy(ServiceTime.now())))
    val uiState: StateFlow<InputUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val stations = repository.getTimetable().stations.sortedBy { it.order }
            _uiState.update {
                it.copy(isLoading = false, stations = stations, selection = exampleSelection(stations))
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

    // 처음 연 사람(심사자 포함)이 바로 계산해 볼 수 있게 예시 경로를 채워 둔다
    private fun exampleSelection(stations: List<Station>): RouteSelection {
        val byName = stations.associateBy { it.name }
        return RouteSelection(from = byName["서울역"], to = byName["인천"], via = byName["구로"])
    }

    companion object {
        val STAY_OPTIONS = listOf(10, 30, 60)

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TeumtaApplication
                InputViewModel(app.timetableRepository)
            }
        }
    }
}
