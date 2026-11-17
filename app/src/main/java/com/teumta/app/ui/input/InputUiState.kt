package com.teumta.app.ui.input

import com.teumta.app.domain.ServiceTime
import com.teumta.app.domain.StationTree
import com.teumta.app.domain.model.LastRoute
import java.time.LocalTime
import java.time.ZonedDateTime

data class InputUiState(
    val isLoading: Boolean = true,
    // 노선도 전체를 갈림길에서 끊은 토막들. 출발·도착을 고를 때 보여 준다
    val sections: List<StationSection> = emptyList(),
    val selection: RouteSelection = RouteSelection(),
    val arriveBy: LocalTime,
    val stayMin: Int = 30,
) {
    val canCalculate: Boolean get() = selection.isComplete

    fun toRouteInput(): RouteInput? {
        val s = selection
        if (!s.isComplete) return null
        return RouteInput(s.from!!.id, s.via!!.id, s.to!!.id, arriveBy, stayMin)
    }
}

// 계산 버튼을 눌렀을 때 결과 화면으로 넘길 값
data class RouteInput(
    val fromId: String,
    val viaId: String,
    val toId: String,
    val arriveBy: LocalTime,
    val stayMin: Int,
)

// 처음 보여 줄 도착 시각: 지금(한국 시각)부터 3시간 뒤를 10분 단위로 올린다.
// 예시 경로(서울역 → 구로 30분 → 인천)는 머무름까지 2시간 넘게 걸려서 넉넉히 잡았다.
// 01~07시대면 첫차 무렵이라 09:00으로 둔다. 언제 열어도 결과가 나오게 하려는 것이다(ExampleRouteTest)
fun defaultArriveBy(now: ZonedDateTime): LocalTime {
    val later = now.withZoneSameInstant(ServiceTime.SEOUL).toLocalTime().plusMinutes(180)
    val roundUp = (later.minute + 9) / 10 * 10
    val rounded = LocalTime.of(later.hour, 0).plusMinutes(roundUp.toLong())
    return if (rounded.hour in 1..7) LocalTime.of(9, 0) else rounded
}

// 저장해 둔 경로를 지금 역 목록으로 되살린다. 역이 없어졌거나 선택 규칙에 맞지 않으면 null(예시 경로를 쓴다).
// 출발 → 도착 → 경유 순서로 tap해서 RouteSelection의 규칙을 그대로 다시 쓴다
fun restoreSelection(tree: StationTree, saved: LastRoute): RouteSelection? {
    val from = tree.find(saved.fromId) ?: return null
    val to = tree.find(saved.toId) ?: return null
    val via = tree.find(saved.viaId) ?: return null
    return RouteSelection(tree).tap(from).tap(to).tap(via).takeIf { it.isComplete }
}
