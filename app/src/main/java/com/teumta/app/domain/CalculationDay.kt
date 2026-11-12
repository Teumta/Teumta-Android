package com.teumta.app.domain

import com.teumta.app.domain.model.DayType
import java.time.LocalDate
import java.time.ZonedDateTime

// 어느 운행일 시간표로 계산할지. nowSec이 있으면 이미 떠난 열차는 답에서 뺀다
data class CalculationDay(
    val date: LocalDate,
    val dayType: DayType,
    val nowSec: Int?,
)

// 도착 시각이 아직 안 지났으면 오늘 운행일(지금 이후 열차만)로 계산한다.
// 이미 지난 시각이거나 nextDay를 요청하면 다음 운행일로 본다. 예) 밤 10시에 "09:00까지" -> 내일 아침
fun chooseCalculationDay(now: ZonedDateTime, arriveBySec: Int, nextDay: Boolean = false): CalculationDay {
    val today = ServiceTime.serviceDate(now)
    val nowSec = ServiceTime.nowSeconds(now)
    if (nextDay || arriveBySec <= nowSec) {
        val tomorrow = today.plusDays(1)
        return CalculationDay(tomorrow, ServiceTime.dayType(tomorrow), nowSec = null)
    }
    return CalculationDay(today, ServiceTime.dayType(today), nowSec)
}
