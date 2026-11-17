package com.teumta.app.ui.input

import com.teumta.app.domain.StationTree
import com.teumta.app.domain.model.LastRoute
import com.teumta.app.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestoreSelectionTest {

    // 역1 ── 역2 ── 역3 ── 역4, 역3에서 역5 ── 역6으로 갈라진다
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
    fun 저장한_경로를_그대로_되살린다() {
        val selection = restoreSelection(tree, LastRoute(fromId = "S1", viaId = "S3", toId = "S6", stayMin = 30))
        assertEquals(RouteSelection(tree, from = s[0], to = s[5], via = s[2]), selection)
    }

    @Test
    fun 연천_방면도_되살린다() {
        val selection = restoreSelection(tree, LastRoute(fromId = "S6", viaId = "S3", toId = "S1", stayMin = 30))
        assertEquals(RouteSelection(tree, from = s[5], to = s[0], via = s[2]), selection)
    }

    @Test
    fun 지금_시간표에_없는_역이면_null() {
        assertNull(restoreSelection(tree, LastRoute(fromId = "S1", viaId = "S3", toId = "X99", stayMin = 30)))
    }

    @Test
    fun 경유역이_두_역을_잇는_길에_없으면_null() {
        // 역4 -> 역6 길은 역3, 역5를 지난다. 역2는 길 밖이다
        assertNull(restoreSelection(tree, LastRoute(fromId = "S4", viaId = "S2", toId = "S6", stayMin = 30)))
    }
}
