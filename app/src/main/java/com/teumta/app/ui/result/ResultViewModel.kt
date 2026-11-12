package com.teumta.app.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.teumta.app.TeumtaApplication
import com.teumta.app.data.TimetableRepository
import com.teumta.app.domain.DepartureCalculator
import com.teumta.app.domain.DepartureRequest
import com.teumta.app.domain.ServiceTime
import com.teumta.app.domain.chooseCalculationDay
import com.teumta.app.ui.navigation.ResultDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime

// 입력 화면에서 받은 값으로 계산해서 결과 또는 실패 이유를 상태로 내놓는다
class ResultViewModel(
    private val input: ResultDestination,
    private val repository: TimetableRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        calculate(nextDay = false)
    }

    // 막차가 이미 떠났을 때 "다음 운행일 같은 시각"으로 다시 계산
    fun showNextDay() {
        calculate(nextDay = true)
    }

    private fun calculate(nextDay: Boolean) {
        viewModelScope.launch {
            val timetable = repository.getTimetable()
            val arriveBySec = ServiceTime.toServiceSeconds(LocalTime.ofSecondOfDay(input.arriveByMinute * 60L))
            val day = chooseCalculationDay(ServiceTime.now(), arriveBySec, nextDay)
            val request = DepartureRequest(
                fromId = input.fromId,
                viaId = input.viaId,
                toId = input.toId,
                dayType = day.dayType,
                arriveBySec = arriveBySec,
                stayMin = input.stayMin,
                nowSec = day.nowSec,
            )
            _uiState.value = ResultUiState.Ready(
                day = day,
                result = DepartureCalculator(timetable).calculate(request),
                stationNames = timetable.stations.associate { it.id to it.name },
                stayMin = input.stayMin,
            )
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TeumtaApplication
                // Navigation이 넘긴 값은 SavedStateHandle에 들어 있다
                ResultViewModel(createSavedStateHandle().toRoute<ResultDestination>(), app.timetableRepository)
            }
        }
    }
}
