package com.teumta.app.ui.input

import com.teumta.app.domain.model.Station

enum class StationRole { FROM, VIA, TO }

// 지금 무엇을 고를 차례인지
enum class SelectStep { FROM, TO, VIA, DONE }

// 노선도에서 고른 세 역. 출발 -> 도착 -> 경유 순서로 고른다.
// 경유역은 출발역과 도착역 사이에서만 고를 수 있어서, 두 역을 먼저 정해야 범위를 알 수 있다.
data class RouteSelection(
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

    // 출발역~도착역 순서 범위 (둘 다 골랐을 때만)
    val range: IntRange?
        get() {
            val a = from?.order ?: return null
            val b = to?.order ?: return null
            return minOf(a, b)..maxOf(a, b)
        }

    // 출발역과 도착역 사이에 고를 수 있는 역이 있는지
    val hasViaCandidate: Boolean get() = range?.let { it.last - it.first >= 2 } ?: true

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

    private fun isBetween(station: Station): Boolean {
        val r = range ?: return false
        return station.order > r.first && station.order < r.last
    }
}
