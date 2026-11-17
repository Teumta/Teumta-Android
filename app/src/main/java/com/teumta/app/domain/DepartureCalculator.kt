package com.teumta.app.domain

import com.teumta.app.domain.model.DayType
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
    val first: Journey,
    val second: Journey,
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

// 도착 시각에서 거꾸로 계산한다. 구간마다 갈아타기도 고려한다.
// 1. 경유역 -> 도착역: 도착 시각까지 도착할 수 있는 가장 늦은 경유역 출발
// 2. 경유역에는 그 출발 − 머무를 시간 − 2분까지 도착해야 한다
// 3. 출발역 -> 경유역: 그 시각까지 경유역에 도착할 수 있는 가장 늦은 출발이 답, 그 앞 출발이 대안
// 출발 시각을 정한 다음에는 제시간에 닿는 경로 중 갈아타기가 가장 적은 것을 고른다
class DepartureCalculator(private val timetable: Timetable) {

    private val transferSecById = timetable.stations.associate { it.id to (it.transferSec ?: TRANSFER_BUFFER_SEC) }

    fun calculate(request: DepartureRequest): DepartureResult {
        require(setOf(request.fromId, request.viaId, request.toId).size == 3) { "출발역, 경유역, 도착역은 서로 달라야 한다" }
        listOf(request.fromId, request.viaId, request.toId).forEach {
            require(it in transferSecById) { "시간표에 없는 역: $it" }
        }
        val search = JourneySearch(
            trains = timetable.trains.filter { it.dayType == request.dayType },
            transferSecOf = { transferSecById.getValue(it) },
        )

        // 1. 경유역 -> 도착역
        val secondDepart = search.latestDepartures(request.viaId, request.toId, request.arriveBySec, limit = 1)
            .firstOrNull() ?: return DepartureResult.Failure(FailReason.NO_TRAIN_BEFORE_ARRIVAL)
        val second = journey(search, request.viaId, request.toId, secondDepart, request.arriveBySec)

        // 2. 경유역 도착 마감
        val viaDeadline = second.departSec - request.stayMin * 60 - TRANSFER_BUFFER_SEC

        // 3. 출발역 -> 경유역 (늦게 떠나는 순으로 두 개)
        val departs = search.latestDepartures(request.fromId, request.viaId, viaDeadline, limit = 2)
        if (departs.isEmpty()) return DepartureResult.Failure(FailReason.STAY_TOO_LONG)

        val now = request.nowSec
        if (now != null && departs[0] < now) {
            return DepartureResult.Failure(FailReason.ALREADY_DEPARTED)
        }
        val alternative = departs.getOrNull(1)?.takeIf { now == null || it >= now }
        return DepartureResult.Success(
            plan = DeparturePlan(journey(search, request.fromId, request.viaId, departs[0], viaDeadline), second),
            alternative = alternative?.let { DeparturePlan(journey(search, request.fromId, request.viaId, it, viaDeadline), second) },
        )
    }

    // latestDepartures가 찾은 시각은 그 시각에 떠나 도착할 수 있다는 뜻이라 경로가 반드시 있다
    private fun journey(search: JourneySearch, fromId: String, toId: String, departAt: Int, deadline: Int): Journey =
        checkNotNull(search.journey(fromId, toId, departAt, deadline)) { "$fromId -> $toId ${departAt}초 출발 경로가 없다" }

    companion object {
        // 내리고 다시 타는 여유 (2분). 경유역에서 쓰고, 갈아타는 시간이 따로 없는 역에서도 쓴다
        const val TRANSFER_BUFFER_SEC = 120
    }
}
