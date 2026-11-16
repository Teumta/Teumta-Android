package com.teumta.app.ui.input

import com.teumta.app.domain.model.LastRoute
import com.teumta.app.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestoreSelectionTest {

    private val s = (1..6).map { Station("S$it", "역$it", it) }

    @Test
    fun 저장한_경로를_그대로_되살린다() {
        val selection = restoreSelection(s,
            LastRoute(fromId = "S1", viaId = "S3", toId = "S5", stayMin = 30)
        )
        assertEquals(RouteSelection(from = s[0], to = s[4], via = s[2]), selection)
    }

    @Test
    fun 서울역_방면도_되살린다() {
        val selection = restoreSelection(s, LastRoute(fromId = "S6", viaId = "S4", toId = "S2", stayMin = 30))
        assertEquals(RouteSelection(from = s[5], to = s[1], via = s[3]), selection)
    }

    @Test
    fun 지금_시간표에_없는_역이면_null() {
        assertNull(restoreSelection(s, LastRoute(fromId = "S1", viaId = "S3", toId = "X99", stayMin = 30)))
    }

    @Test
    fun 경유역이_사이에_없으면_null() {
        assertNull(restoreSelection(s, LastRoute(fromId = "S1", viaId = "S6", toId = "S3", stayMin = 30)))
    }
}
