package com.teumta.app.domain

import com.teumta.app.domain.model.DayType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ServiceTimeTest {

    @Test
    fun 새벽_3시_전은_전날_운행일() {
        val night = ZonedDateTime.of(2026, 11, 12, 0, 40, 0, 0, ServiceTime.SEOUL)
        val morning = ZonedDateTime.of(2026, 11, 12, 3, 0, 0, 0, ServiceTime.SEOUL)

        assertEquals(LocalDate.of(2026, 11, 11), ServiceTime.serviceDate(night))
        assertEquals(LocalDate.of(2026, 11, 12), ServiceTime.serviceDate(morning))
    }

    @Test
    fun 해외_시간대에서도_한국_시각으로_계산한다() {
        // 뉴욕 11/11 10:40 = 한국 11/12 00:40 -> 한국 운행일은 11/11, 24시 40분
        val newYork = ZonedDateTime.of(2026, 11, 11, 10, 40, 0, 0, ZoneId.of("America/New_York"))

        assertEquals(LocalDate.of(2026, 11, 11), ServiceTime.serviceDate(newYork))
        assertEquals(24 * 3600 + 40 * 60, ServiceTime.nowSeconds(newYork))
    }

    @Test
    fun 자정_넘은_시각은_24시_이후로() {
        assertEquals(24 * 3600 + 30 * 60, ServiceTime.toServiceSeconds(LocalTime.of(0, 30)))
        assertEquals(21 * 3600, ServiceTime.toServiceSeconds(LocalTime.of(21, 0)))
    }

    @Test
    fun 토요일_일요일_공휴일은_휴일_시간표() {
        assertEquals(DayType.WEEKDAY, ServiceTime.dayType(LocalDate.of(2026, 11, 11))) // 수
        assertEquals(DayType.HOLIDAY, ServiceTime.dayType(LocalDate.of(2026, 11, 14))) // 토
        assertEquals(DayType.HOLIDAY, ServiceTime.dayType(LocalDate.of(2026, 11, 15))) // 일
        assertEquals(DayType.HOLIDAY, ServiceTime.dayType(LocalDate.of(2026, 12, 25))) // 금, 성탄절
    }
}