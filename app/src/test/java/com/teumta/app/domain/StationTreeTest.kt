package com.teumta.app.domain

import com.teumta.app.data.Line1Assets
import com.teumta.app.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/*손으로 확인할 수 있게 만든 작은 나무. C에서 두 갈래로 갈라진다.

  A ── B ── C ── D
            └── E ── F*/

class StationTreeTest {

    private val tree = StationTree(
        listOf(
            Station("A", "A", 1),
            Station("B", "B", 2, parentId = "A"),
            Station("C", "C", 3, parentId = "B"),
            Station("D", "D", 4, parentId = "C"),
            Station("E", "E", 5, parentId = "C"),
            Station("F", "F", 6, parentId = "E"),
        ),
    )

    @Test
    fun 같은_줄이면_순서대로_지난다() {
        assertEquals(listOf("A", "B", "C", "D"), tree.path("A", "D").map { it.id })
        // 반대 방향은 거꾸로
        assertEquals(listOf("D", "C", "B", "A"), tree.path("D", "A").map { it.id })
    }

    @Test
    fun 다른_갈래로_가면_갈라지는_역을_지난다() {
        // D에서 F로 가려면 C까지 올라갔다가 E 쪽으로 내려간다. A, B는 지나지 않는다
        assertEquals(listOf("D", "C", "E", "F"), tree.path("D", "F").map { it.id })
    }

    @Test
    fun 같은_역이거나_없는_역이면() {
        assertEquals(listOf("C"), tree.path("C", "C").map { it.id })
        assertEquals(emptyList<Station>(), tree.path("A", "X"))
    }

    @Test
    fun 실제_1호선_인천에서_광명은_구로와_금천구청을_지난다() {
        val line1 = StationTree(Line1Assets.timetable.stations)
        val id = line1.stations.associate { it.name to it.id }
        val names = line1.path(id.getValue("인천"), id.getValue("광명")).map { it.name }

        assertEquals("인천", names.first())
        assertEquals("광명", names.last())
        assertTrue("구로" in names && "가산디지털단지" in names && "금천구청" in names)
        // 구로에서 바로 경부선 쪽으로 가니 신도림은 지나지 않는다
        assertFalse("신도림" in names)
        // 인천~구로 21개 + 가산디지털단지·독산·금천구청 3개 + 광명 1개
        assertEquals(25, names.size)
    }
}
