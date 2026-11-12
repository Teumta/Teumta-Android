package com.teumta.app.ui

import com.teumta.app.domain.model.DayType
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// 운행일 기준 초 -> "20:11". 24시가 넘으면 00시부터 다시 센다. 초는 버린다(출발은 일찍 보여 주는 쪽이 안전)
fun formatClock(serviceSec: Int): String {
    val hour = serviceSec / 3600 % 24
    val minute = serviceSec % 3600 / 60
    return "%02d:%02d".format(hour, minute)
}

// 초 -> "36분", "1시간", "1시간 30분"
fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}분"
        m == 0 -> "${h}시간"
        else -> "${h}시간 ${m}분"
    }
}

// "11월 12일 (목) · 평일 시간표"
fun formatServiceDay(date: LocalDate, dayType: DayType): String {
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
    val type = if (dayType == DayType.WEEKDAY) "평일" else "휴일"
    return "${date.monthValue}월 ${date.dayOfMonth}일 ($weekday) · $type 시간표"
}
