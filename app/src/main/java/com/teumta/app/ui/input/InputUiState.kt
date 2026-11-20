package com.teumta.app.ui.input

import com.teumta.app.domain.ServiceTime
import com.teumta.app.domain.StationTree
import com.teumta.app.domain.model.LastRoute
import com.teumta.app.domain.model.Station
import java.time.LocalTime
import java.time.ZonedDateTime

data class InputUiState(
    val isLoading: Boolean = true,
    // 노선도 전체를 갈림길에서 끊은 토막들. 출발·도착을 고르는 화면에 보여 준다
    val sections: List<StationSection> = emptyList(),
    val selection: RouteSelection = RouteSelection(),
    val arriveBy: LocalTime,
    val stayMin: Int = 30,
    // 로그인해서 저장해 둔 자주 쓰는 두 역(서버 이름은 home·work). 둘 다 있을 때만 두 역 버튼을 보여 준다
    val home: Station? = null,
    val work: Station? = null,
    // 바뀐 개인정보처리방침 안내를 아직 보지 않았으면 true(앱을 연 뒤 한 번 보여 준다)
    val showPolicyNotice: Boolean = false,
    // 결과 화면에서 돌아오며 열어 달라고 한 것(도착 시각 시트, 들를 역 고르기). 연 뒤에는 null로 비운다
    val pendingOpen: InputOpenRequest? = null,
) {
    val canCalculate: Boolean get() = selection.isComplete

    fun toRouteInput(): RouteInput? {
        val from = selection.from ?: return null
        val to = selection.to ?: return null
        return RouteInput(from.id, selection.via?.id, to.id, arriveBy, stayMin)
    }
}

// 결과 화면의 "도착 시각 늦추기", "중간에 들렀다 가기"가 입력 화면에 열어 달라고 하는 것
enum class InputOpenRequest { ARRIVE_SHEET, VIA_PICKER }

// 계산 버튼을 눌렀을 때 결과 화면으로 넘길 값
data class RouteInput(
    val fromId: String,
    // null이면 들르지 않고 바로 간다
    val viaId: String?,
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

// 저장해 둔 경로를 지금 역 목록으로 되살린다. 출발·도착 역이 없어졌으면 null(예시 경로를 쓴다).
// 들를 역만 없어졌거나 두 역 사이가 아니면 들르지 않는 경로로 되살린다. 규칙은 RouteSelection.pick을 그대로 쓴다
fun restoreSelection(tree: StationTree, saved: LastRoute): RouteSelection? {
    val from = tree.find(saved.fromId) ?: return null
    val to = tree.find(saved.toId) ?: return null
    if (from == to) return null
    val route = RouteSelection(tree, from = from, to = to)
    val via = saved.viaId?.let(tree::find) ?: return route
    return route.pick(StationRole.VIA, via)
}

enum class CommuteDirection { TO_WORK, TO_HOME }

// 두 역 버튼. TO_WORK는 home → work, TO_HOME은 work → home 방향으로 출발·도착을 채우고 들를 역은 비운다(바로 가기).
// 들를 곳은 날마다 달라서 남겨 두지 않는다. 두 역 중 하나라도 없으면 null
fun InputUiState.commuteSelection(direction: CommuteDirection): RouteSelection? {
    val (from, to) = when (direction) {
        CommuteDirection.TO_WORK -> home to work
        CommuteDirection.TO_HOME -> work to home
    }
    if (from == null || to == null) return null
    if (from == to) return null
    return RouteSelection(selection.tree, from = from, to = to)
}
