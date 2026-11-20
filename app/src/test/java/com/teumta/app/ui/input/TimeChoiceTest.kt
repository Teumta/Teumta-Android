package com.teumta.app.ui.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class TimeChoiceTest {

    @Test
    fun 머무를_시간은_0분부터_5시간_55분까지_5분_단위() {
        assertTrue(StayChoice.isValid(0))
        assertTrue(StayChoice.isValid(85))
        assertTrue(StayChoice.isValid(355))
        assertFalse(StayChoice.isValid(-5))
        assertFalse(StayChoice.isValid(360))
        assertFalse(StayChoice.isValid(12))
    }

    @Test
    fun 바퀴_두_개와_분을_오간다() {
        // 1시간 20분 = 시간 칸 1, 분 칸 4(20분)
        assertEquals(80, StayChoice.of(hours = 1, minuteIndex = 4))
        assertEquals(1, StayChoice.hoursOf(80))
        assertEquals(4, StayChoice.minuteIndexOf(80))
        // 바퀴 끝 칸끼리 고르면 가장 긴 시간
        assertEquals(StayChoice.MAX_MIN, StayChoice.of(StayChoice.HOUR_COUNT - 1, StayChoice.MINUTE_COUNT - 1))
    }

    @Test
    fun 입력_화면과_시트의_칩은_모두_고를_수_있는_값이다() {
        (StayChoice.CHIPS + StayChoice.SHEET_CHIPS).forEach { assertTrue("$it", StayChoice.isValid(it)) }
    }

    @Test
    fun 도착_시각_빠른_칩은_1시간_뒤_첫_정각이나_30분부터_30분_간격() {
        assertEquals(
            listOf("15:30", "16:00", "16:30", "17:00"),
            arriveQuickPicks(LocalTime.of(14, 7)).map { it.toString() },
        )
        // 1시간 뒤가 딱 정각이면 그 시각부터
        assertEquals("15:00", arriveQuickPicks(LocalTime.of(14, 0)).first().toString())
    }

    @Test
    fun 자정을_넘으면_00시부터_다시_센다() {
        assertEquals(
            listOf("00:00", "00:30", "01:00", "01:30"),
            arriveQuickPicks(LocalTime.of(22, 45)).map { it.toString() },
        )
    }
}
