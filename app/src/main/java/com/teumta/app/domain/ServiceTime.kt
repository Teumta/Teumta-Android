package com.teumta.app.domain

import com.teumta.app.domain.model.DayType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

// 시간표는 "운행일" 기준이다. 새벽 3시 전은 전날 운행일이고, 그 시각은 24시 이후로 센다(00:30 -> 24:30).
// 기기 시간대와 상관없이 항상 한국 시각(Asia/Seoul)으로 계산한다. 해외에서 앱을 열어도 같은 답이 나온다.
object ServiceTime {
    val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
    private const val DAY_START_HOUR = 3
    private const val SECONDS_PER_DAY = 86_400

    // 공휴일. 2026-11-06 시간표를 쓰는 동안 남은 날만 넣었다 (서버를 붙이면 서버가 판단)
    private val HOLIDAYS = setOf(
        LocalDate.of(2026, 12, 25),
        LocalDate.of(2027, 1, 1),
    )

    fun now(): ZonedDateTime = ZonedDateTime.now(SEOUL)

    // 지금이 속한 운행일
    fun serviceDate(now: ZonedDateTime): LocalDate {
        val seoul = now.withZoneSameInstant(SEOUL)
        return if (seoul.hour < DAY_START_HOUR) seoul.toLocalDate().minusDays(1) else seoul.toLocalDate()
    }

    // 시계 시각 -> 운행일 기준 초. 새벽 3시 전이면 전날 운행일의 24시 이후로 본다
    fun toServiceSeconds(time: LocalTime): Int {
        val seconds = time.toSecondOfDay()
        return if (time.hour < DAY_START_HOUR) seconds + SECONDS_PER_DAY else seconds
    }

    // 지금 시각을 운행일 기준 초로
    fun nowSeconds(now: ZonedDateTime): Int = toServiceSeconds(now.withZoneSameInstant(SEOUL).toLocalTime())

    // 토·일·공휴일은 휴일 시간표. TAGO에 토요일 시간표가 없어서 토요일도 휴일로 본다
    fun dayType(serviceDate: LocalDate): DayType = when {
        serviceDate.dayOfWeek == DayOfWeek.SATURDAY -> DayType.HOLIDAY
        serviceDate.dayOfWeek == DayOfWeek.SUNDAY -> DayType.HOLIDAY
        serviceDate in HOLIDAYS -> DayType.HOLIDAY
        else -> DayType.WEEKDAY
    }
}
