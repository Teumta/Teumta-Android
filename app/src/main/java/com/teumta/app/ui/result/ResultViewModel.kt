package com.teumta.app.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teumta.app.data.TimetableRepository
import com.teumta.app.domain.DepartureCalculator
import com.teumta.app.domain.DepartureRequest
import com.teumta.app.domain.DepartureResult
import com.teumta.app.domain.FailReason
import com.teumta.app.domain.ServiceTime
import com.teumta.app.domain.chooseCalculationDay
import com.teumta.app.ui.navigation.ResultDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import javax.inject.Inject

// 입력 화면에서 받은 값으로 계산해서 결과 또는 실패 이유를 상태로 내놓는다
@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TimetableRepository,
) : ViewModel() {

    // Navigation이 넘긴 값은 SavedStateHandle에 들어 있다. Hilt가 이 화면의 것을 넣어 준다
    private val input: ResultDestination = savedStateHandle.toRoute()
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
            // 102개 역 시간표는 구간이 수만 개라 계산을 화면(Main) 스레드 밖에서 한다
            val ready = withContext(Dispatchers.Default) {
                val calculator = DepartureCalculator(timetable)
                val result = calculator.calculate(request)
                // 머무를 시간이 모자라면 "머무를 시간 줄이기"에 쓸 최대 시간을, 들르는 길이 막히면 바로 가는 답을 함께 구한다
                val tooLong = result == DepartureResult.Failure(FailReason.STAY_TOO_LONG)
                val viaBlocked = input.viaId != null && result is DepartureResult.Failure && result.reason != FailReason.NO_TRAIN_BEFORE_ARRIVAL
                ResultUiState.Ready(
                    day = day,
                    result = result,
                    stationNames = timetable.stations.associate { it.id to it.name },
                    fromId = input.fromId,
                    viaId = input.viaId,
                    toId = input.toId,
                    arriveBySec = arriveBySec,
                    stayMin = input.stayMin,
                    maxStaySec = if (tooLong) calculator.maxStaySec(request) else null,
                    directPlan = if (viaBlocked) (calculator.calculate(request.copy(viaId = null)) as? DepartureResult.Success)?.plan else null,
                    timetableSource = timetable.source,
                    baseDate = timetable.baseDate,
                )
            }
            _uiState.value = ready
        }
    }
}
