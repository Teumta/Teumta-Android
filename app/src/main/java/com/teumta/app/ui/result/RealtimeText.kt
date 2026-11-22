package com.teumta.app.ui.result

import com.teumta.app.domain.DepartureResult
import com.teumta.app.domain.model.RealtimeArrival
import com.teumta.app.domain.model.TrainPosition
import java.time.Duration
import java.time.Instant

// 실시간 열차를 보여 줄 때: 오늘 계산이고 그 역에서 타는 시각이 이 시간 안일 때만. 더 뒤에 타는 역에는 지금 오는 열차가 상관없다
const val REALTIME_WINDOW_SEC = 60 * 60

// 서버에 물을 탈 역과 그 열차에서 내릴 역. 동선의 "OO 승차" 줄마다 하나
data class RealtimeTarget(val stationId: String, val towardStationId: String)

// 동선 순서대로, 실시간을 물을 타는 역들. 들르거나 갈아타면 여러 개다
fun realtimeTargets(state: ResultUiState.Ready): List<RealtimeTarget> {
    val plan = (state.result as? DepartureResult.Success)?.plan ?: return emptyList()
    val now = state.day.nowSec ?: return emptyList()
    return (plan.first.rides + plan.second?.rides.orEmpty())
        .filter { it.departSec - now <= REALTIME_WINDOW_SEC }
        .map { RealtimeTarget(it.fromId, it.toId) }
        .distinct()
}

// 열차가 어디쯤인지. 탈 역(당역)과 바로 전 역(전역)은 역 안내판처럼 짧게, 더 멀면 몇 역 전과 지금 역을 쓴다.
// 예) "당역 도착", "전역 출발", "3역 전 · 신길 출발"
fun realtimePositionText(arrival: RealtimeArrival): String = when (arrival.stationsAway) {
    0 -> when (arrival.position) {
        TrainPosition.ARRIVED -> "당역 도착"
        TrainPosition.ENTERING -> "당역 진입"
        TrainPosition.DEPARTED -> "당역 출발"
        TrainPosition.PREV_DEPARTED -> "전역 출발"
    }
    1 -> when (arrival.position) {
        TrainPosition.ARRIVED -> "전역 도착"
        TrainPosition.ENTERING -> "전역 진입"
        TrainPosition.DEPARTED -> "전역 출발"
        TrainPosition.PREV_DEPARTED -> "전전역 출발"
    }
    else -> {
        val station = arrival.currentStation
        val where = when (arrival.position) {
            TrainPosition.ENTERING -> "$station 진입"
            TrainPosition.ARRIVED -> "$station 도착"
            TrainPosition.DEPARTED -> "$station 출발"
            TrainPosition.PREV_DEPARTED -> "${withRo(station)} 가는 중"
        }
        "${arrival.stationsAway}역 전 · $where"
    }
}

// 위치를 받은 지 얼마나 됐는지. 서울시 위치는 몇 분 늦게 올 때가 있어서 줄마다 적는다
fun realtimeAgeText(receivedAt: Instant, now: Instant): String {
    val minutes = Duration.between(receivedAt, now).toMinutes()
    return if (minutes < 1) "방금 위치" else "${minutes}분 전 위치"
}

// 역 이름 + 로/으로. 받침이 있으면 "으로"(신도림으로), 없거나 ㄹ 받침이면 "로"(구로로, 서울역으로는 받침 ㄱ이라 으로)
fun withRo(name: String): String {
    val last = name.lastOrNull() ?: return name
    if (last !in '가'..'힣') return "${name}로"
    val jong = (last - '가') % 28
    return if (jong == 0 || jong == 8) "${name}로" else "${name}으로"
}
