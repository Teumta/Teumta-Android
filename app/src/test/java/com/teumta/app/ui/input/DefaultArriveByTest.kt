package com.teumta.app.ui.input

import com.teumta.app.domain.ServiceTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DefaultArriveByTest {

    private fun seoul(hour: Int, minute: Int) = ZonedDateTime.of(2026, 11, 12, hour, minute, 0, 0, ServiceTime.SEOUL)

    @Test
    fun 지금부터_1시간30분_뒤를_10분_단위로_올린다() {
        assertEquals(LocalTime.of(20, 0), defaultArriveBy(seoul(18, 21)))
        assertEquals(LocalTime.of(19, 50), defaultArriveBy(seoul(18, 20)))
    }

    @Test
    fun 자정_넘어도_막차_시간대면_그대로() {
        assertEquals(LocalTime.of(0, 30), defaultArriveBy(seoul(22, 55)))
    }

    @Test
    fun 열차가_없는_새벽이면_9시() {
        assertEquals(LocalTime.of(9, 0), defaultArriveBy(seoul(0, 30)))
        assertEquals(LocalTime.of(9, 0), defaultArriveBy(seoul(5, 0)))
    }

    @Test
    fun 해외에서_열어도_한국_시각_기준() {
        // 뉴욕 11/12 04:00 = 한국 11/12 18:00 → 19:30
        val newYork = ZonedDateTime.of(2026, 11, 12, 4, 0, 0, 0, ZoneId.of("America/New_York"))
        assertEquals(LocalTime.of(19, 30), defaultArriveBy(newYork))
    }
}
