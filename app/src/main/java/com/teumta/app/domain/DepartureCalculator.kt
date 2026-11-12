package com.teumta.app.domain

import com.teumta.app.domain.model.DayType
import com.teumta.app.domain.model.Direction
import com.teumta.app.domain.model.Timetable
import com.teumta.app.domain.model.Train

// 계산에 넣는 값. 시각은 모두 운행일 기준 초
data class DepartureRequest(
    val fromId: String,
    val viaId: String,
    val toId: String,
    val dayType: DayType,
    // 도착역에 이 시각까지 도착해야 한다
    val arriveBySec: Int,
    // 경유역에서 머무를 시간(분)
    val stayMin: Int,
    // 지금 시각. 넣으면 이미 떠난 열차는 답이 될 수 없다
    val nowSec: Int? = null,
)

// 열차 한 편을 타고 가는 구간
data class Ride(
    val train: Train,
    val fromId: String,
    val departSec: Int,
    val toId: String,
    val arriveSec: Int,
)

// 답: 출발역 -> 경유역(머무름) -> 도착역
data class DeparturePlan(
    val first: Ride,
    val second: Ride,
) {
    // 출발역에서 타야 하는 시각
    val departSec: Int get() = first.departSec

    // 경유역에서 실제로 머물 수 있는 시간(초). 내리고 타는 여유는 뺐다
    val staySec: Int get() = second.departSec - first.arriveSec - DepartureCalculator.TRANSFER_BUFFER_SEC
}

enum class FailReason {
    // 도착 시각 전에 도착역에 가는 열차가 없다 (첫차 이전)
    NO_TRAIN_BEFORE_ARRIVAL,

    // 경유역에서 그만큼 머물면 탈 수 있는 열차가 없다 (머무를 시간 과다)
    STAY_TOO_LONG,

    // 탈 수 있는 마지막 열차가 이미 떠났다 (막차 이후)
    ALREADY_DEPARTED,
}

sealed interface DepartureResult {
    data class Success(val plan: DeparturePlan, val alternative: DeparturePlan?) : DepartureResult
    data class Failure(val reason: FailReason) : DepartureResult
}

// 도착 시각에서 거꾸로 계산한다.
// 1. 경유역 -> 도착역: 도착 시각 전에 도착하는 열차 중 경유역에서 가장 늦게 떠나는 열차
// 2. 경유역에는 그 열차 출발 − 머무를 시간 − 2분까지 도착해야 한다
// 3. 출발역-> 경유역: 그 시각 전에 경유역에 도착하는 열차 중 가장 늦게 떠나는 열차가 답, 그 앞 열차가 대안
class DepartureCalculator(private val timetable: Timetable) {

    private val orderById = timetable.stations.associate { it.id to it.order }

    fun calculate(request: DepartureRequest): DepartureResult {
        val fromOrder = orderOf(request.fromId)
        val viaOrder = orderOf(request.viaId)
        val toOrder = orderOf(request.toId)
        require(viaOrder in minOf(fromOrder, toOrder) + 1 until maxOf(fromOrder, toOrder)) {
            "경유역은 출발역과 도착역 사이에 있어야 한다"
        }
        val direction = if (fromOrder < toOrder) Direction.DOWN else Direction.UP
        val trains = timetable.trains.filter { it.dayType == request.dayType && it.direction == direction }

        // 1. 경유역 -> 도착역
        val second = rides(trains, request.viaId, request.toId)
            .filter { it.arriveSec <= request.arriveBySec }
            .maxByOrNull { it.departSec }
            ?: return DepartureResult.Failure(FailReason.NO_TRAIN_BEFORE_ARRIVAL)

        // 2. 경유역 도착 마감
        val viaDeadline = second.departSec - request.stayMin * 60 - TRANSFER_BUFFER_SEC

        // 3. 출발역 -> 경유역 (늦게 떠나는 순)
        val firsts = rides(trains, request.fromId, request.viaId)
            .filter { it.arriveSec <= viaDeadline }
            .sortedByDescending { it.departSec }
        if (firsts.isEmpty()) return DepartureResult.Failure(FailReason.STAY_TOO_LONG)

        val now = request.nowSec
        if (now != null && firsts[0].departSec < now) {
            return DepartureResult.Failure(FailReason.ALREADY_DEPARTED)
        }
        val alternative = firsts.getOrNull(1)?.takeIf { now == null || it.departSec >= now }
        return DepartureResult.Success(
            plan = DeparturePlan(firsts[0], second),
            alternative = alternative?.let { DeparturePlan(it, second) },
        )
    }

    // from에서 타서 to에서 내릴 수 있는 열차 구간 목록
    private fun rides(trains: List<Train>, fromId: String, toId: String): List<Ride> = trains.mapNotNull { train ->
        val boardIndex = train.stops.indexOfFirst { it.stationId == fromId }
        val alightIndex = train.stops.indexOfFirst { it.stationId == toId }
        if (boardIndex == -1 || alightIndex == -1 || boardIndex >= alightIndex) return@mapNotNull null
        Ride(
            train = train,
            fromId = fromId,
            departSec = train.stops[boardIndex].dep,
            toId = toId,
            arriveSec = train.stops[alightIndex].arr,
        )
    }

    private fun orderOf(stationId: String): Int =
        requireNotNull(orderById[stationId]) { "시간표에 없는 역: $stationId" }

    companion object {
        // 경유역에서 내리고 다시 타는 여유 (2분)
        const val TRANSFER_BUFFER_SEC = 120
    }
}
