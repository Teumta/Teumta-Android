package com.teumta.app.data

import com.teumta.app.domain.model.DayType
import com.teumta.app.domain.model.Direction
import org.junit.Assert.assertEquals
import org.junit.Test

// 서버 응답 모양(GET /api/stations, GET /api/trains)을 작게 만들어 읽어 본다
class TimetableParserTest {

    private val stations = """
        [{"id":"A","name":"A역","branch":"본선","order":1,"parentId":null,"expressStop":false,"transferSec":null},
         {"id":"B","name":"B역","branch":"본선","order":2,"parentId":"A","expressStop":true,"transferSec":240}]
    """.trimIndent()

    private fun trains(dayType: String, id: Long, baseDate: String = "2026-11-06") = """
        {"baseDate":"$baseDate","dayType":"$dayType","trains":[
          {"id":$id,"direction":"DOWN","headsign":"B","stops":[
            {"stationId":"A","arr":19620,"dep":19620},{"stationId":"B","arr":19800,"dep":19830}]}]}
    """.trimIndent()

    @Test
    fun 서버_응답_세_개를_시간표_하나로_합친다() {
        val timetable = TimetableParser.parseServer(stations, listOf(trains("WEEKDAY", 1), trains("HOLIDAY", 2)))

        assertEquals("2026-11-06", timetable.baseDate)
        // 앱이 모르는 칸(branch, parentId, expressStop)은 건너뛰고, 아는 칸만 읽는다
        assertEquals(240, timetable.stations[1].transferSec)
        // 요일 종류는 바깥에서 열차마다 넣고, 숫자 ID는 문자열로 바꾼다
        val (weekday, holiday) = timetable.trains
        assertEquals("1", weekday.id)
        assertEquals(DayType.WEEKDAY, weekday.dayType)
        assertEquals(DayType.HOLIDAY, holiday.dayType)
        assertEquals(Direction.DOWN, holiday.direction)
        assertEquals(19830, holiday.stops[1].dep)
    }

    @Test(expected = IllegalArgumentException::class)
    fun 평일과_휴일의_기준일이_다르면_예외() {
        TimetableParser.parseServer(stations, listOf(trains("WEEKDAY", 1), trains("HOLIDAY", 2, baseDate = "2026-12-01")))
    }
}
