package com.teumta.app.ui.input

import com.teumta.app.domain.StationTree
import com.teumta.app.domain.model.Station

enum class StationRole { FROM, VIA, TO }

// 지금 무엇을 고를 차례인지
enum class SelectStep { FROM, TO, VIA, DONE }

// 노선도에서 고른 세 역. 출발 -> 도착 -> 경유 순서로 고른다.
// 경유역은 출발역과 도착역 사이(나무에서 두 역을 잇는 길 위)에서만 고를 수 있어서, 두 역을 먼저 정해야 범위를 알 수 있다.
data class RouteSelection(
    // 1호선 역들의 나무 모양. 두 역 사이의 길을 여기서 찾는다
    val tree: StationTree = StationTree(emptyList()),
    val from: Station? = null,
    val to: Station? = null,
    val via: Station? = null,
) {
    val step: SelectStep
        get() = when {
            from == null -> SelectStep.FROM
            to == null -> SelectStep.TO
            via == null -> SelectStep.VIA
            else -> SelectStep.DONE
        }

    val isComplete: Boolean get() = step == SelectStep.DONE

    // 출발역부터 도착역까지 지나는 역들 (둘 다 골랐을 때만). 역마다 isEnabled를 부르므로 만들 때 한 번만 구한다
    val path: List<Station>? = if (from != null && to != null) tree.path(from.id, to.id) else null

    // 출발역과 도착역 사이에 고를 수 있는 역이 있는지
    val hasViaCandidate: Boolean get() = path?.let { it.size >= 3 } ?: true

    fun roleOf(station: Station): StationRole? = when (station) {
        from -> StationRole.FROM
        via -> StationRole.VIA
        to -> StationRole.TO
        else -> null
    }

    // 지금 누를 수 있는 역인지. 이미 고른 역은 해제할 수 있게 항상 누를 수 있다
    fun isEnabled(station: Station): Boolean = when {
        roleOf(station) != null -> true
        step == SelectStep.VIA -> isBetween(station)
        step == SelectStep.DONE -> false
        else -> true
    }

    // 역을 눌렀을 때의 다음 상태. 고른 역을 다시 누르면 해제하고,
    // 출발·도착이 바뀌면 경유역 범위가 달라지므로 경유역도 함께 해제한다
    fun tap(station: Station): RouteSelection = when (station) {
        via -> copy(via = null)
        from -> copy(from = null, via = null)
        to -> copy(to = null, via = null)
        else -> when (step) {
            SelectStep.FROM -> copy(from = station)
            SelectStep.TO -> copy(to = station)
            SelectStep.VIA -> if (isBetween(station)) copy(via = station) else this
            SelectStep.DONE -> this
        }
    }

    // 두 역을 잇는 길 위에 있고, 양 끝(출발·도착)은 아닌 역
    private fun isBetween(station: Station): Boolean =
        station != from && station != to && path?.contains(station) == true
}
