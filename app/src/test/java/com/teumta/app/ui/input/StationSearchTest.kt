package com.teumta.app.ui.input

import com.teumta.app.data.Line1Assets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 실제 1호선 102개 역으로 검색을 확인한다
class StationSearchTest {

    private val stations = Line1Assets.timetable.stations

    private fun names(query: String) = searchStations(stations, query).map { it.station.name }

    @Test
    fun 이름_일부로_찾는다() {
        assertEquals(listOf("구로"), names("구로"))
        assertTrue("가산디지털단지" in names("디지털"))
    }

    @Test
    fun 초성으로_찾는다() {
        assertTrue("구로" in names("ㄱㄹ"))
        assertTrue("동인천" in names("ㄷㅇㅊ"))
    }

    @Test
    fun 글자와_초성을_섞어도_찾는다() {
        // "가"까지 치고 다음 글자는 초성만 친 상태
        assertEquals(listOf("가산디지털단지"), names("가ㅅ"))
    }

    @Test
    fun 이름_앞부분이_맞는_역이_먼저_나온다() {
        // "인천"으로 시작하는 역이 "동인천"보다 앞
        val result = names("인천")
        assertEquals("인천", result.first())
        assertTrue("동인천" in result)
    }

    @Test
    fun 맞은_자리를_알려_준다() {
        val match = searchStations(stations, "인천").single { it.station.name == "동인천" }
        assertEquals(1, match.start)
        assertEquals(2, match.length)
    }

    @Test
    fun 빈칸은_무시하고_없는_역은_빈_목록() {
        assertEquals(listOf("구로"), names(" 구 로 "))
        assertTrue(names("강남").isEmpty())
        assertTrue(names("").isEmpty())
    }

    @Test
    fun 초성_계산() {
        assertEquals('ㄱ', choseongOf('구'))
        assertEquals('ㅎ', choseongOf('힣'))
        assertEquals(null, choseongOf('A'))
    }
}
