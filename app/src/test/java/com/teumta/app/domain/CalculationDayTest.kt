package com.teumta.app.domain

import com.teumta.app.domain.model.DayType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class CalculationDayTest {

    private fun seoul(day: Int, hour: Int, minute: Int) = ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, ServiceTime.SEOUL)

    private fun sec(hour: Int, minute: Int) = hour * 3600 + minute * 60

    @Test
    fun 도착_시각이_아직_안_지났으면_오늘_지금_이후_열차로() {
        // 목 11/12 18:00, 21:00까지
        val day = chooseCalculationDay(seoul(12, 18, 0), sec(21, 0))

        assertEquals(LocalDate.of(2026, 11, 12), day.date)
        assertEquals(DayType.WEEKDAY, day.dayType)
        assertEquals(sec(18, 0), day.nowSec)
    }

    @Test
    fun 이미_지난_시각이면_다음_운행일() {
        // 금 11/13 22:00에 09:00까지 -> 토 11/14 아침, 휴일 시간표
        val day = chooseCalculationDay(seoul(13, 22, 0), sec(9, 0))

        assertEquals(LocalDate.of(2026, 11, 14), day.date)
        assertEquals(DayType.HOLIDAY, day.dayType)
        assertNull(day.nowSec)
    }

    @Test
    fun 자정_넘은_새벽에_아침_시각을_고르면_달력상_오늘_아침() {
        // 금 11/13 00:40은 목요일 운행일. 09:00까지 -> 금 11/13 아침
        val day = chooseCalculationDay(seoul(13, 0, 40), sec(9, 0))

        assertEquals(LocalDate.of(2026, 11, 13), day.date)
        assertEquals(DayType.WEEKDAY, day.dayType)
    }

    @Test
    fun 자정_넘은_도착_시각도_오늘_운행일() {
        // 목 23:00에 00:30까지(운행일 기준 24:30)
        val day = chooseCalculationDay(seoul(12, 23, 0), sec(24, 30))
        assertEquals(LocalDate.of(2026, 11, 12), day.date)
    }

    @Test
    fun 다음_운행일을_요청하면_내일로() {
        val day = chooseCalculationDay(seoul(12, 18, 0), sec(21, 0), nextDay = true)

        assertEquals(LocalDate.of(2026, 11, 13), day.date)
        assertNull(day.nowSec)
    }
}
