package com.teumta.app.ui.input

import com.teumta.app.domain.StationTree
import com.teumta.app.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/*역 1~6. 3에서 두 갈래로 갈라진다.

  역1 ── 역2 ── 역3 ── 역4
               └── 역5 ── 역6*/

class RouteSelectionTest {

    private val s = listOf(
        Station("S1", "역1", 1),
        Station("S2", "역2", 2, parentId = "S1"),
        Station("S3", "역3", 3, parentId = "S2"),
        Station("S4", "역4", 4, parentId = "S3"),
        Station("S5", "역5", 5, parentId = "S3"),
        Station("S6", "역6", 6, parentId = "S5"),
    )
    private val tree = StationTree(s)
    private val route = RouteSelection(tree, from = s[0], to = s[3])

    @Test
    fun 들를_역이_없어도_출발과_도착만_있으면_계산할_수_있다() {
        assertTrue(route.isComplete)
        assertNull(route.via)
        assertFalse(RouteSelection(tree, from = s[0]).isComplete)
    }

    @Test
    fun 칸마다_역을_넣는다() {
        val picked = RouteSelection(tree)
            .pick(StationRole.TO, s[3])
            .pick(StationRole.FROM, s[0])
            .pick(StationRole.VIA, s[1])

        assertEquals(RouteSelection(tree, from = s[0], to = s[3], via = s[1]), picked)
    }

    @Test
    fun 출발과_도착은_같은_역일_수_없다() {
        assertFalse(route.canPick(StationRole.FROM, s[3]))
        assertFalse(route.canPick(StationRole.TO, s[0]))
        assertEquals(route, route.pick(StationRole.TO, s[0]))
    }

    @Test
    fun 들를_역은_두_역을_잇는_길_위에서만_고른다() {
        // 역4 -> 역6: 역3에서 갈래를 바꿔 역4, 역3, 역5, 역6을 지난다
        val selection = RouteSelection(tree, from = s[3], to = s[5])

        assertEquals(listOf(s[3], s[2], s[4], s[5]), selection.path)
        assertEquals(listOf(s[2], s[4]), selection.viaCandidates)
        // 역2는 순서(order)로는 역4와 역6 "사이"처럼 보여도 길에서 벗어난 역이라 고를 수 없다
        assertFalse(selection.canPick(StationRole.VIA, s[1]))
        assertNull(selection.pick(StationRole.VIA, s[1]).via)
        // 양 끝도 들를 역이 될 수 없다
        assertFalse(selection.canPick(StationRole.VIA, s[3]))
    }

    @Test
    fun 출발이나_도착을_바꿔_들를_역이_길에서_벗어나면_바로_가기가_된다() {
        val full = route.pick(StationRole.VIA, s[1]) // 역1 -> 역2 -> 역4

        // 출발을 역3으로: 역3 -> 역4 길에 역2가 없다
        assertNull(full.pick(StationRole.FROM, s[2]).via)
        // 도착을 역6으로: 역1 -> 역6 길에도 역2가 있어서 남는다
        assertEquals(s[1], full.pick(StationRole.TO, s[5]).via)
        // 들를 역을 출발로 옮기면 끝 역이 되어 비운다
        assertNull(full.pick(StationRole.FROM, s[1]).via)
    }

    @Test
    fun 바꾸기는_출발과_도착을_맞바꾸고_들를_역은_남긴다() {
        val swapped = route.pick(StationRole.VIA, s[1]).swapped()
        assertEquals(RouteSelection(tree, from = s[3], to = s[0], via = s[1]), swapped)
    }

    @Test
    fun 들를_역을_지우면_바로_가기() {
        assertEquals(route, route.pick(StationRole.VIA, s[1]).withoutVia())
    }

    @Test
    fun 이웃한_두_역이면_들를_역_후보가_없다() {
        // 역3과 역5는 갈래가 달라도 바로 이웃이다
        assertTrue(RouteSelection(tree, from = s[2], to = s[4]).viaCandidates.isEmpty())
        assertEquals(listOf(s[2]), RouteSelection(tree, from = s[3], to = s[4]).viaCandidates)
    }
}
