package com.teumta.app.domain

import com.teumta.app.data.TimetableParser
import com.teumta.app.domain.model.DayType
import com.teumta.app.domain.model.Timetable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/*실제 assets/timetable.json으로 계산한다. 기대값은 같은 규칙의 Python 계산과 timetable.json을 직접 보고 확인했다.
단위 테스트는 app 모듈 폴더에서 실행돼서 상대 경로로 파일을 읽는다.*/

class RealTimetableTest {

    private val timetable: Timetable = TimetableParser.parse(File("src/main/assets/timetable.json").readText())
    private val calculator = DepartureCalculator(timetable)
    private val idByName = timetable.stations.associate { it.name to it.id }

    @Test
    fun 시간표를_읽는다() {
        assertEquals(29, timetable.stations.size)
        assertEquals(453, timetable.trains.size)
    }

    @Test
    fun 평일_서울역에서_구로_30분_들렀다_21시까지_인천() {
        val result = success(DayType.WEEKDAY, "서울역", "구로", "인천", arriveBy = 21 * 3600, stayMin = 30)

        assertEquals(sec("19:14"), result.plan.departSec)
        assertEquals(sec("19:34"), result.plan.first.arriveSec)
        assertEquals(sec("20:11:30"), result.plan.second.departSec)
        assertEquals(sec("20:59"), result.plan.second.arriveSec)
        assertEquals(sec("19:03:30"), result.alternative?.departSec)
    }

    @Test
    fun 휴일_인천에서_부평_1시간_들렀다_18시까지_용산으로_서울역_방면() {
        val result = success(DayType.HOLIDAY, "인천", "부평", "용산", arriveBy = 18 * 3600, stayMin = 60)

        assertEquals(sec("15:45"), result.plan.departSec)
        assertEquals(sec("17:14:30"), result.plan.second.departSec)
        assertEquals(sec("17:55"), result.plan.second.arriveSec)
    }

    @Test
    fun 평일_자정_넘어_00시30분까지_인천() {
        val result = success(DayType.WEEKDAY, "구로", "부평", "인천", arriveBy = sec("24:30"), stayMin = 30)

        assertEquals(sec("22:56:30"), result.plan.departSec)
        assertEquals(sec("23:59"), result.plan.second.departSec)
        assertEquals(sec("24:21"), result.plan.second.arriveSec)
    }

    @Test
    fun 첫차_전_도착_시각과_너무_긴_머무름은_실패() {
        assertEquals(
            DepartureResult.Failure(FailReason.NO_TRAIN_BEFORE_ARRIVAL),
            calculate(DayType.WEEKDAY, "서울역", "구로", "인천", arriveBy = sec("05:30"), stayMin = 30),
        )
        assertEquals(
            DepartureResult.Failure(FailReason.STAY_TOO_LONG),
            calculate(DayType.WEEKDAY, "서울역", "구로", "인천", arriveBy = sec("07:00"), stayMin = 180),
        )
    }

    private fun calculate(day: DayType, from: String, via: String, to: String, arriveBy: Int, stayMin: Int) =
        calculator.calculate(DepartureRequest(idByName.getValue(from), idByName.getValue(via), idByName.getValue(to), day, arriveBy, stayMin))

    private fun success(day: DayType, from: String, via: String, to: String, arriveBy: Int, stayMin: Int): DepartureResult.Success {
        val result = calculate(day, from, via, to, arriveBy, stayMin)
        assertTrue("성공해야 하는데 $result", result is DepartureResult.Success)
        return result as DepartureResult.Success
    }

    private fun sec(time: String): Int {
        val p = time.split(":").map { it.toInt() }
        return p[0] * 3600 + p[1] * 60 + p.getOrElse(2) { 0 }
    }
}
