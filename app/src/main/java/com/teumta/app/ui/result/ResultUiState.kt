package com.teumta.app.ui.result

import com.teumta.app.domain.CalculationDay
import com.teumta.app.domain.DeparturePlan
import com.teumta.app.domain.DepartureResult
import com.teumta.app.domain.model.RealtimeArrival
import com.teumta.app.domain.model.TimetableSource
import java.time.Instant

sealed interface ResultUiState {
    data object Loading : ResultUiState

    data class Ready(
        val day: CalculationDay,
        val result: DepartureResult,
        val stationNames: Map<String, String>,
        val fromId: String,
        // 들를 역. 바로 가면 null
        val viaId: String?,
        val toId: String,
        // 목표: 도착역에 이 시각까지 (운행일 기준 초)
        val arriveBySec: Int,
        val stayMin: Int,
        // "머무를 시간이 모자라요"일 때만 채운다: 가장 오래 머무를 수 있는 시간(초, 0분도 안 되면 null)과
        // 들르지 않고 바로 갈 때의 답(그것도 안 되면 null). 대신 해 볼 일을 보여 주는 데 쓴다
        val maxStaySec: Int? = null,
        val directPlan: DeparturePlan? = null,
        // 계산에 쓴 시간표가 어디서 왔고 언제 기준인지
        val timetableSource: TimetableSource,
        val baseDate: String,
    ) : ResultUiState {
        fun nameOf(stationId: String): String = stationNames[stationId] ?: stationId

        val fromName: String get() = nameOf(fromId)
        val toName: String get() = nameOf(toId)
        val viaName: String? get() = viaId?.let(::nameOf)
    }
}

// 타는 역 한 곳으로 지금 오는 열차. 가까운 순서로 두 편까지. checkedAt은 앱이 받아 온 시각(몇 분 전 위치인지 셀 때 쓴다)
data class RealtimeTrains(val arrivals: List<RealtimeArrival>, val checkedAt: Instant)
