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

    @Test
    fun 출발_도착_경유_순서로_고른다() {
        val selection = RouteSelection(tree).tap(s[0]).tap(s[3]).tap(s[1])

        assertEquals(s[0], selection.from)
        assertEquals(s[3], selection.to)
        assertEquals(s[1], selection.via)
        assertTrue(selection.isComplete)
    }

    @Test
    fun 연천_방면도_고를_수_있다() {
        val selection = RouteSelection(tree).tap(s[5]).tap(s[0]).tap(s[2])
        assertTrue(selection.isComplete)
    }

    @Test
    fun 경유역은_두_역을_잇는_길_위에서만_고른다() {
        // 역4 -> 역6: 역3에서 갈래를 바꿔 역4, 역3, 역5, 역6을 지난다
        val selection = RouteSelection(tree).tap(s[3]).tap(s[5])

        assertEquals(listOf(s[3], s[2], s[4], s[5]), selection.path)
        assertTrue(selection.isEnabled(s[2]))
        assertTrue(selection.isEnabled(s[4]))
        // 역2는 순서(order)로는 역4와 역6 "사이"가 아니지만, 길에서 벗어난 역이라 고를 수 없다
        assertFalse(selection.isEnabled(s[1]))
        assertNull(selection.tap(s[1]).via)
    }

    @Test
    fun 다시_누르면_해제된다() {
        val full = RouteSelection(tree).tap(s[0]).tap(s[3]).tap(s[1])

        assertNull(full.tap(s[1]).via)
        // 출발역이나 도착역을 해제하면 경유역 범위가 바뀌므로 경유역도 해제
        assertEquals(RouteSelection(tree, to = s[3]), full.tap(s[0]))
        assertEquals(RouteSelection(tree, from = s[0]), full.tap(s[3]))
    }

    @Test
    fun 이웃한_두_역이면_경유역_후보가_없다() {
        // 역3과 역5는 갈래가 달라도 바로 이웃이다
        assertFalse(RouteSelection(tree).tap(s[2]).tap(s[4]).hasViaCandidate)
        assertTrue(RouteSelection(tree).tap(s[3]).tap(s[4]).hasViaCandidate)
    }

    @Test
    fun 세_역을_모두_고르면_다른_역은_누를_수_없다() {
        val full = RouteSelection(tree).tap(s[0]).tap(s[3]).tap(s[1])

        assertFalse(full.isEnabled(s[2]))
        assertTrue(full.isEnabled(s[0]))
    }
}
