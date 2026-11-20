package com.teumta.app.ui.input

import java.time.LocalTime

// 머무를 시간을 고르는 규칙. 0분부터 5시간 55분까지 5분 단위다(바퀴: 시간 0~5, 분 00~55).
// 0분은 내려서 바로 다음 열차를 탄다는 뜻이다(내리고 타는 여유 2분은 계산이 따로 더한다)
object StayChoice {
    const val STEP_MIN = 5
    const val MAX_HOURS = 5
    const val MAX_MIN = MAX_HOURS * 60 + 55

    // 바퀴의 칸 수: 시간 0~5 → 6칸, 분 00~55 → 12칸
    const val HOUR_COUNT = MAX_HOURS + 1
    const val MINUTE_COUNT = 60 / STEP_MIN

    // 입력 화면에 바로 보이는 칩
    val CHIPS = listOf(10, 30, 60)

    // 시트 위쪽 빠른 칩
    val SHEET_CHIPS = listOf(0, 10, 30, 60)

    // 저장해 둔 값이 지금 규칙에 맞는지(앱을 고친 뒤 예전 값이 남아 있을 수 있다)
    fun isValid(minutes: Int): Boolean = minutes in 0..MAX_MIN && minutes % STEP_MIN == 0

    // 바퀴 두 개(시간 칸, 분 칸)를 분으로
    fun of(hours: Int, minuteIndex: Int): Int = hours * 60 + minuteIndex * STEP_MIN

    fun hoursOf(minutes: Int): Int = minutes / 60

    fun minuteIndexOf(minutes: Int): Int = minutes % 60 / STEP_MIN
}

// 도착 시각 시트의 빠른 칩. 지금부터 1시간 뒤 첫 정각이나 30분부터 30분 간격으로 네 개.
// 열차를 타고 가는 시간이 있어서 지금 바로 다음 시각은 고를 일이 드물다. 자정을 넘으면 00시부터 다시 센다
fun arriveQuickPicks(now: LocalTime): List<LocalTime> {
    val later = now.plusMinutes(60)
    val minuteOfDay = later.hour * 60 + later.minute
    val first = (minuteOfDay + 29) / 30 * 30
    return List(4) { i -> LocalTime.MIDNIGHT.plusMinutes((first + i * 30).toLong()) }
}
