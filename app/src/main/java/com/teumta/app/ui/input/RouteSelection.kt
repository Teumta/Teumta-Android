package com.teumta.app.ui.input

import com.teumta.app.domain.StationTree
import com.teumta.app.domain.model.Station

// 경로 상자의 세 칸. 칸을 누르면 그 칸의 역을 고르는 화면이 열린다
enum class StationRole { FROM, VIA, TO }

// 경로 상자에 고른 역들. 출발·도착은 꼭 있어야 하고, 들를 역은 없어도 된다(바로 가기).
// 들를 역은 출발역과 도착역 사이(나무에서 두 역을 잇는 길 위)에서만 고를 수 있다
data class RouteSelection(
    // 1호선 역들의 나무 모양. 두 역 사이의 길을 여기서 찾는다
    val tree: StationTree = StationTree(emptyList()),
    val from: Station? = null,
    val to: Station? = null,
    val via: Station? = null,
) {
    // 출발과 도착만 있으면 계산할 수 있다. 들를 역이 없으면 바로 가는 열차를 찾는다
    val isComplete: Boolean get() = from != null && to != null

    // 출발역부터 도착역까지 지나는 역들 (둘 다 골랐을 때만). 고르는 화면이 역마다 물어보므로 만들 때 한 번만 구한다
    val path: List<Station>? = if (from != null && to != null) tree.path(from.id, to.id) else null

    // 들를 역 후보: 길 위의 역 중 양 끝(출발·도착)을 뺀 것. 이웃한 두 역이면 비어 있다
    val viaCandidates: List<Station> = path?.drop(1)?.dropLast(1).orEmpty()

    fun roleOf(station: Station): StationRole? = when (station) {
        from -> StationRole.FROM
        via -> StationRole.VIA
        to -> StationRole.TO
        else -> null
    }

    fun stationOf(role: StationRole): Station? = when (role) {
        StationRole.FROM -> from
        StationRole.VIA -> via
        StationRole.TO -> to
    }

    // 이 칸에 고를 수 있는 역인지. 출발과 도착은 같은 역일 수 없고, 들를 역은 두 역 사이에서만
    fun canPick(role: StationRole, station: Station): Boolean = when (role) {
        StationRole.FROM -> station != to
        StationRole.TO -> station != from
        StationRole.VIA -> station in viaCandidates
    }

    // 칸에 역을 넣은 다음 상태. 고를 수 없는 역이면 그대로 둔다.
    // 출발·도착이 바뀌어 들를 역이 더는 두 역 사이가 아니면 들를 역을 비운다(바로 가기가 된다)
    fun pick(role: StationRole, station: Station): RouteSelection {
        if (!canPick(role, station)) return this
        return when (role) {
            StationRole.FROM -> copy(from = station).dropViaIfOffPath()
            StationRole.TO -> copy(to = station).dropViaIfOffPath()
            StationRole.VIA -> copy(via = station)
        }
    }

    // 들르지 않고 바로 가기
    fun withoutVia(): RouteSelection = copy(via = null)

    // 출발과 도착을 맞바꾼다. 같은 길을 거꾸로 가므로 들를 역은 그대로 사이에 있다
    fun swapped(): RouteSelection = copy(from = to, to = from)

    private fun dropViaIfOffPath(): RouteSelection =
        if (via == null || via in viaCandidates) this else copy(via = null)
}
