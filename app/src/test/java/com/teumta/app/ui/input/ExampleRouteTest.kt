package com.teumta.app.ui.input

import com.teumta.app.data.TimetableParser
import com.teumta.app.domain.DepartureCalculator
import com.teumta.app.domain.DepartureRequest
import com.teumta.app.domain.DepartureResult
import com.teumta.app.domain.ServiceTime
import com.teumta.app.domain.chooseCalculationDay
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.ZonedDateTime

// 심사자가 언제 앱을 열어도, 예시 경로와 기본값 그대로 계산하면 결과가 나와야 한다
class ExampleRouteTest {

    private val timetable = TimetableParser.parse(File("src/main/assets/timetable.json").readText())
    private val calculator = DepartureCalculator(timetable)
    private val idByName = timetable.stations.associate { it.name to it.id }

    @Test
    fun 평일과_휴일_하루_종일_10분마다_예시_경로가_계산된다() {
        // 목 11/12(평일)와 토 11/14(휴일) 03:00부터 하루
        for (day in listOf(12, 14)) {
            var now = ZonedDateTime.of(2026, 11, day, 3, 0, 0, 0, ServiceTime.SEOUL)
            repeat(24 * 6) {
                val arriveBy = defaultArriveBy(now)
                val arriveBySec = ServiceTime.toServiceSeconds(arriveBy)
                val calcDay = chooseCalculationDay(now, arriveBySec)
                val result = calculator.calculate(
                    DepartureRequest(
                        idByName.getValue("서울역"), idByName.getValue("구로"), idByName.getValue("인천"),
                        calcDay.dayType, arriveBySec, stayMin = 30, nowSec = calcDay.nowSec,
                    ),
                )
                assertTrue("$now 에 열면 $arriveBy 까지 → $result", result is DepartureResult.Success)
                now = now.plusMinutes(10)
            }
        }
    }
}
