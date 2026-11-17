package com.teumta.app.domain

import com.teumta.app.domain.model.Train

// 한 역에서 다른 역까지 타고 가는 열차들. 갈아타지 않으면 열차가 하나다
data class Journey(val rides: List<Ride>) {
    val fromId: String get() = rides.first().fromId
    val toId: String get() = rides.last().toId
    val departSec: Int get() = rides.first().departSec
    val arriveSec: Int get() = rides.last().arriveSec
}

// 연결 하나: 한 열차가 이웃한 두 정차역 사이를 달리는 구간. seq는 그 열차 안에서 몇 번째 구간인지
private class Connection(
    val train: Train,
    val seq: Int,
    val fromId: String,
    val toId: String,
    val dep: Int,
    val arr: Int,
)

// 갈아타기까지 고려해서 경로를 찾는다 (Connection Scan Algorithm).
// 모든 열차를 "이웃한 두 역 사이 구간"으로 잘라 출발 시각 순으로 한 줄로 세워 두고, 그 줄을 한 번 훑어 답을 낸다.
// 노선 모양(갈림길)을 몰라도 열차가 실제로 다니는 구간만 따라가므로, 1호선처럼 갈라지는 노선도 그대로 계산된다.
class JourneySearch(
    trains: List<Train>,
    // 역에서 다른 열차로 갈아탈 때 걸리는 시간(초)
    private val transferSecOf: (stationId: String) -> Int,
) {
    // 출발 시각 순. 같으면 도착 시각, 그것도 같으면 열차 안 순서로 정해서, 거꾸로 훑을 때 한 열차의 뒤 구간이 먼저 나온다
    private val connections: List<Connection> = trains
        .flatMap { train ->
            train.stops.zipWithNext().mapIndexed { i, (a, b) ->
                Connection(train, i, a.stationId, b.stationId, a.dep, b.arr)
            }
        }
        .sortedWith(compareBy<Connection>({ it.dep }, { it.arr }, { it.seq }))

    // from에서 떠나 to에 deadline까지 도착할 수 있는 출발 시각을 늦은 순으로 limit개까지.
    // 줄을 뒤에서부터(늦게 떠나는 구간부터) 훑으며 "이 역에서 늦어도 언제 떠나면 되는지"를 채운다
    fun latestDepartures(fromId: String, toId: String, deadline: Int, limit: Int): List<Int> {
        // 역 -> 그 역에서 늦어도 떠나야 하는 시각. 늦은 구간부터 보므로 처음 채운 값이 가장 늦은 값이다
        val latest = HashMap<String, Int>()
        // 이 열차를 타고 있으면 제시간에 도착할 수 있는 열차들
        val reachableTrains = HashSet<String>()
        val found = mutableListOf<Int>()

        for (c in connections.asReversed()) {
            // 출발역으로 돌아오는 구간은 쓰지 않는다. 거꾸로 한 정거장 갔다가 같은 열차를 다시 타는 엉뚱한 경로를 막는다
            if (c.arr > deadline || c.toId == fromId) continue
            val reachable = c.toId == toId ||
                    c.train.id in reachableTrains ||
                    latest[c.toId]?.let { c.arr + transferSecOf(c.toId) <= it } == true
            if (!reachable) continue

            reachableTrains += c.train.id
            latest.putIfAbsent(c.fromId, c.dep)
            if (c.fromId == fromId) {
                // 이 열차의 출발역 앞쪽 구간은 출발역을 지나 다시 오는 경로라 이어 주지 않는다
                reachableTrains -= c.train.id
                // 같은 시각에 떠나는 열차가 둘이면 한 번만 센다 (대안은 "더 일찍" 떠나야 의미가 있다)
                if (found.lastOrNull() != c.dep) found += c.dep
                if (found.size == limit) break
            }
        }
        return found
    }

    // from에서 정확히 departAt에 떠나 to에 deadline까지 도착하는 경로 중 갈아타기가 가장 적은 것. 없으면 null.
    // 열차를 1편만 타는 경우부터 판(round)을 하나씩 늘리며 찾는다. 같은 판에서는 가장 일찍 도착하는 것을 고른다.
    // 1분 일찍 도착하려고 두 번 더 갈아타는 경로보다, 덜 갈아타고 제시간에 닿는 경로가 쓰기 편하다
    fun journey(fromId: String, toId: String, departAt: Int, deadline: Int): Journey? {
        // 역 -> 지난 판까지 그 역에 가장 일찍 닿는 방법
        var previous = mapOf<String, Arrival>()
        repeat(MAX_RIDES) {
            // 이번 판: 지난 판의 결과에서 열차를 한 편 더 타서 닿는 곳
            val current = HashMap(previous)
            // 열차 -> 이번 판에 그 열차를 탄 구간
            val boardedAt = HashMap<String, Connection>()

            for (c in connections) {
                if (c.dep < departAt) continue
                val best = current[toId]?.time
                if (best != null && c.dep >= best) break // 이후 구간은 지금 답보다 일찍 도착할 수 없다
                // latestDepartures와 같은 규칙: 출발역으로 돌아오는 구간은 쓰지 않고, 그 열차에서는 내린 것으로 본다
                if (c.toId == fromId) {
                    boardedAt.remove(c.train.id)
                    continue
                }
                val canBoardHere = if (c.fromId == fromId) {
                    // 출발역에서는 정한 시각에 떠나는 열차만 탄다
                    c.dep == departAt
                } else {
                    previous[c.fromId]?.let { it.time + transferSecOf(c.fromId) <= c.dep } == true
                }
                // 이미 탄 열차도 더 뒤 역에서 탈 수 있으면 그 역에서 탄 것으로 바꾼다.
                // 도착 시각은 같고, 갈아타는 역을 지나쳤다가 되돌아오는 길이 없어진다 (예: 신도림까지 갔다가 구로로 되돌아오기)
                if (canBoardHere) boardedAt[c.train.id] = c
                val board = boardedAt[c.train.id] ?: continue

                if (c.arr < (current[c.toId]?.time ?: Int.MAX_VALUE)) {
                    val before = previous[board.fromId]?.rides.orEmpty()
                    current[c.toId] = Arrival(c.arr, before + Ride(board.train, board.fromId, board.dep, c.toId, c.arr))
                }
            }
            current[toId]?.takeIf { it.time <= deadline }?.let { return Journey(it.rides) }
            previous = current
        }
        return null
    }

    // 어느 역에 닿은 시각과, 거기까지 탄 열차들
    private class Arrival(val time: Int, val rides: List<Ride>)

    private companion object {
        // 한 구간에서 최대 몇 편까지 타 보는지. 1호선은 갈림길이 셋이라 보통 3편 안에 간다
        const val MAX_RIDES = 6
    }
}
