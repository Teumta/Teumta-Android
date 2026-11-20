package com.teumta.app.ui.input

import com.teumta.app.domain.StationTree
import com.teumta.app.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

// 두 역 버튼이 출발·도착을 맞게 채우는지
class FavoriteSelectionTest {

    // 한 줄로 이어진 다섯 역 (앞 역을 parent로): 서울역 - 남영 - 용산 - 노량진 - 대방
    private val stations = listOf("서울역", "남영", "용산", "노량진", "대방").mapIndexed { i, name ->
        Station("S$i", name, i + 1, parentId = if (i == 0) null else "S${i - 1}")
    }
    private val tree = StationTree(stations)
    private val favoriteOrigin = stations[4] // 대방
    private val favoriteDestination = stations[0] // 서울역

    // 지난번 경로(남영 → 노량진, 용산 들르기)가 골라져 있던 상태
    private val state = InputUiState(
        isLoading = false,
        selection = RouteSelection(tree, from = stations[1], to = stations[3], via = stations[2]),
        arriveBy = LocalTime.of(9, 0),
        favoriteOrigin = favoriteOrigin,
        favoriteDestination = favoriteDestination,
    )

    @Test
    fun 가는_길은_출발역에서_도착역으로_채우고_들를_역은_비운다() {
        val selection = state.favoriteSelection(FavoriteDirection.FORWARD)!!
        assertEquals(favoriteOrigin, selection.from)
        assertEquals(favoriteDestination, selection.to)
        // 지난번에 들른 용산은 남기지 않는다. 바로 계산할 수 있다
        assertNull(selection.via)
        assertTrue(selection.isComplete)
    }

    @Test
    fun 오는_길은_도착역에서_출발역으로_채운다() {
        val selection = state.favoriteSelection(FavoriteDirection.BACKWARD)!!
        assertEquals(favoriteDestination, selection.from)
        assertEquals(favoriteOrigin, selection.to)
    }

    @Test
    fun 두_역_중_하나라도_없으면_바꾸지_않는다() {
        assertNull(state.copy(favoriteDestination = null).favoriteSelection(FavoriteDirection.FORWARD))
        assertNull(state.copy(favoriteOrigin = null).favoriteSelection(FavoriteDirection.BACKWARD))
    }
}
