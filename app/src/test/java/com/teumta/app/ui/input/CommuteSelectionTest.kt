package com.teumta.app.ui.input

import com.teumta.app.domain.StationTree
import com.teumta.app.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

// 출근길·퇴근길 버튼이 출발·도착을 맞게 채우는지
class CommuteSelectionTest {

    // 한 줄로 이어진 다섯 역 (앞 역을 parent로): 서울역 - 남영 - 용산 - 노량진 - 대방
    private val stations = listOf("서울역", "남영", "용산", "노량진", "대방").mapIndexed { i, name ->
        Station("S$i", name, i + 1, parentId = if (i == 0) null else "S${i - 1}")
    }
    private val tree = StationTree(stations)
    private val home = stations[4] // 대방
    private val work = stations[0] // 서울역

    // 지난번 경로(남영 → 노량진, 용산 들르기)가 골라져 있던 상태
    private val state = InputUiState(
        isLoading = false,
        selection = RouteSelection(tree, from = stations[1], to = stations[3], via = stations[2]),
        arriveBy = LocalTime.of(9, 0),
        home = home,
        work = work,
    )

    @Test
    fun 출근길은_집에서_회사로_채우고_들를_역을_고를_차례가_된다() {
        val selection = state.commuteSelection(CommuteDirection.TO_WORK)!!
        assertEquals(home, selection.from)
        assertEquals(work, selection.to)
        assertNull(selection.via)
        assertEquals(SelectStep.VIA, selection.step)
    }

    @Test
    fun 퇴근길은_회사에서_집으로_채운다() {
        val selection = state.commuteSelection(CommuteDirection.TO_HOME)!!
        assertEquals(work, selection.from)
        assertEquals(home, selection.to)
    }

    @Test
    fun 집역이나_회사역이_없으면_바꾸지_않는다() {
        assertNull(state.copy(work = null).commuteSelection(CommuteDirection.TO_WORK))
        assertNull(state.copy(home = null).commuteSelection(CommuteDirection.TO_HOME))
    }
}
