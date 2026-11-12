package com.teumta.app.ui.input

import com.teumta.app.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteSelectionTest {

    // 역 1~6
    private val s = (1..6).map { Station("S$it", "역$it", it) }

    @Test
    fun 출발_도착_경유_순서로_고른다() {
        val selection = RouteSelection().tap(s[0]).tap(s[4]).tap(s[2])

        assertEquals(s[0], selection.from)
        assertEquals(s[4], selection.to)
        assertEquals(s[2], selection.via)
        assertTrue(selection.isComplete)
    }

    @Test
    fun 서울역_방면도_고를_수_있다() {
        val selection = RouteSelection().tap(s[5]).tap(s[1]).tap(s[3])
        assertTrue(selection.isComplete)
    }

    @Test
    fun 경유역은_출발역과_도착역_사이만_고른다() {
        val selection = RouteSelection().tap(s[1]).tap(s[3])

        assertFalse(selection.isEnabled(s[0]))
        assertTrue(selection.isEnabled(s[2]))
        assertFalse(selection.isEnabled(s[4]))
        assertNull(selection.tap(s[4]).via)
    }

    @Test
    fun 다시_누르면_해제된다() {
        val full = RouteSelection().tap(s[0]).tap(s[4]).tap(s[2])

        assertNull(full.tap(s[2]).via)
        // 출발역이나 도착역을 해제하면 경유역 범위가 바뀌므로 경유역도 해제
        assertEquals(RouteSelection(to = s[4]), full.tap(s[0]))
        assertEquals(RouteSelection(from = s[0]), full.tap(s[4]))
    }

    @Test
    fun 이웃한_두_역이면_경유역_후보가_없다() {
        assertFalse(RouteSelection().tap(s[1]).tap(s[2]).hasViaCandidate)
        assertTrue(RouteSelection().tap(s[1]).tap(s[3]).hasViaCandidate)
    }

    @Test
    fun 세_역을_모두_고르면_다른_역은_누를_수_없다() {
        val full = RouteSelection().tap(s[0]).tap(s[4]).tap(s[2])

        assertFalse(full.isEnabled(s[3]))
        assertTrue(full.isEnabled(s[0]))
    }
}
