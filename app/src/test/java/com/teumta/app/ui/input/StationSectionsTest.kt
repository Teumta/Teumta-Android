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
                "연천 ~ 구로",
                "구로에서 갈라짐 · 구일 ~ 인천",
                "구로에서 갈라짐 · 가산디지털단지 ~ 금천구청",
                // 짧은 갈래가 먼저 나와서 광명이 금천구청 바로 아래에 보인다
                "금천구청에서 갈라짐 · 광명",
                "금천구청에서 갈라짐 · 석수 ~ 병점",
                "병점에서 갈라짐 · 서동탄",
                "병점에서 갈라짐 · 세마 ~ 신창",
            ),
            sections.map { it.title },
        )
        // 102개 역이 빠짐없이, 한 번씩만 들어간다
        assertEquals(stations.map { it.id }.sorted(), sections.flatMap { it.stations }.map { it.id }.sorted())
    }
}
