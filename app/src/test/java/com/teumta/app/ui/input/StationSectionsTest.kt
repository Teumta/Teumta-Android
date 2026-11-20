package com.teumta.app.ui.input

import com.teumta.app.data.Line1Assets
import com.teumta.app.domain.StationTree
import org.junit.Assert.assertEquals
import org.junit.Test

class StationSectionsTest {

    @Test
    fun 실제_1호선을_갈림길에서_끊는다() {
        val stations = Line1Assets.timetable.stations
        val sections = stationSections(StationTree(stations))

        assertEquals(
            listOf(
                "연천 – 구로",
                "구로 – 인천",
                "구로 – 금천구청",
                // 짧은 갈래가 먼저 나와서 광명이 금천구청 바로 아래에 보인다
                "금천구청 – 광명",
                "금천구청 – 병점",
                "병점 – 서동탄",
                "병점 – 신창",
            ),
            sections.map { it.title },
        )
        // 102개 역이 빠짐없이, 한 번씩만 들어간다
        assertEquals(stations.map { it.id }.sorted(), sections.flatMap { it.stations }.map { it.id }.sorted())
    }
}
