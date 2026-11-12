package com.teumta.app.ui

import com.teumta.app.domain.model.DayType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TimeTextTest {

    @Test
    fun 운행일_초를_시각으로() {
        assertEquals("20:11", formatClock(20 * 3600 + 11 * 60 + 30))
        assertEquals("00:30", formatClock(24 * 3600 + 30 * 60))
        assertEquals("05:07", formatClock(5 * 3600 + 7 * 60))
    }

    @Test
    fun 걸리는_시간을_글로() {
        assertEquals("36분", formatDuration(36 * 60 + 30))
        assertEquals("1시간", formatDuration(3600))
        assertEquals("1시간 30분", formatDuration(90 * 60))
    }

    @Test
    fun 운행일_표시() {
        assertEquals("11월 14일 (토) · 휴일 시간표", formatServiceDay(LocalDate.of(2026, 11, 14), DayType.HOLIDAY))
    }
}
