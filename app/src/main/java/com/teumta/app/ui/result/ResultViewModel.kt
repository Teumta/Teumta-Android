package com.teumta.app.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teumta.app.data.TimetableRepository
import com.teumta.app.data.realtime.RealtimeRepository
import com.teumta.app.domain.DepartureCalculator
import com.teumta.app.domain.DepartureRequest
import com.teumta.app.domain.DepartureResult
import com.teumta.app.domain.FailReason
import com.teumta.app.domain.ServiceTime
import com.teumta.app.domain.chooseCalculationDay
import com.teumta.app.ui.navigation.ResultDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject

// 입력 화면에서 받은 값으로 계산해서 결과 또는 실패 이유를 상태로 내놓는다
@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TimetableRepository,
    private val realtimeRepository: RealtimeRepository,
) : ViewModel() {

    // Navigation이 넘긴 값은 SavedStateHandle에 들어 있다. Hilt가 이 화면의 것을 넣어 준다
    private val input: ResultDestination = savedStateHandle.toRoute()
    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    // 타는 역(동선의 "OO 승차" 줄)마다 지금 오는 열차. 못 받았거나 보여 줄 때가 아닌 역은 빠진다(그 줄만 숨김)
    private val _realtime = MutableStateFlow<Map<RealtimeTarget, RealtimeTrains>>(emptyMap())
    val realtime: StateFlow<Map<RealtimeTarget, RealtimeTrains>> = _realtime.asStateFlow()
    private var realtimeJob: Job? = null

    // 실시간을 받아 오는 중인지. 새로고침 버튼 자리에 도는 표시를 띄운다
    private val _realtimeRefreshing = MutableStateFlow(false)
    val realtimeRefreshing: StateFlow<Boolean> = _realtimeRefreshing.asStateFlow()

    init {
        calculate(nextDay = false)
    }

    // 막차가 이미 떠났을 때 "다음 운행일 같은 시각"으로 다시 계산
    fun showNextDay() {
        calculate(nextDay = true)
    }

    // 타는 역마다 오는 열차를 다시 받는다. 화면이 보이는 동안 1분마다 부른다(서버는 90초 동안 같은 위치를 같이 쓴다)
    fun refreshRealtime() {
        val ready = _uiState.value as? ResultUiState.Ready ?: return
        val targets = realtimeTargets(ready)
        if (targets.isEmpty()) {
            _realtime.value = emptyMap()
            return
        }
        if (realtimeJob?.isActive == true) return
        _realtimeRefreshing.value = true
        realtimeJob = viewModelScope.launch {
            // 역마다 따로 묻되 동시에 묻고, 다 받으면 한 번에 바꾼다(줄이 하나씩 깜빡이지 않게)
            val results = targets.map { target ->
                async { target to realtimeRepository.getArrivals(target.stationId, target.towardStationId) }
            }.awaitAll()
            val checkedAt = Instant.now()
            _realtime.value = results.mapNotNull { (target, arrivals) ->
                arrivals?.let { target to RealtimeTrains(it.take(2), checkedAt) }
            }.toMap()
        }.also { job -> job.invokeOnCompletion { _realtimeRefreshing.value = false } }
    }

    private fun calculate(nextDay: Boolean) {
        realtimeJob?.cancel()
        _realtime.value = emptyMap()
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
            refreshRealtime()
        }
    }
}
