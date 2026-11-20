package com.teumta.app.domain

import com.teumta.app.data.Line1Assets
import com.teumta.app.domain.model.DayType
import com.teumta.app.domain.model.Timetable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/*1호선 102개 역 시간표로 계산한다. 앱 안 assets/line1에는 서버 응답(GET /api/stations, GET /api/trains)을 그대로 저장해 두었다.
1호선은 구로·금천구청·병점에서 갈라지고 급행도 있어서, 갈아타기를 해야 하는 경우가 생긴다.
기대값은 작업 폴더에서 모든 출발 열차를 하나씩 대 보는 별도 계산과 맞춰 확인했다.*/

class Line1TimetableTest {

    private val timetable: Timetable = Line1Assets.timetable
    private val calculator = DepartureCalculator(timetable)
    private val idByName = timetable.stations.associate { it.name to it.id }
    private val nameById = timetable.stations.associate { it.id to it.name }

    @Test
    fun 시간표를_읽는다() {
        assertEquals(102, timetable.stations.size)
        assertEquals(1574, timetable.trains.size)
        assertEquals(240, timetable.stations.single { it.name == "구로" }.transferSec)
        // 광명은 금천구청에서 갈라진다
        assertEquals(idByName["금천구청"], timetable.stations.single { it.name == "광명" }.parentId)
    }

    @Test
    fun 평일_인천에서_부평_30분_들렀다_18시까지_광명_구로에서_갈아탄다() {
        val result = success(DayType.WEEKDAY, "인천", "부평", "광명", arriveBy = sec("18:00"), stayMin = 30)

        assertEquals(sec("15:29"), result.plan.departSec)
        // 부평 -> 구로(용산행), 구로 -> 광명(광명행)
        val second = result.plan.second!!.rides
        assertEquals(listOf("부평" to "구로", "구로" to "광명"), second.map { nameById[it.fromId] to nameById[it.toId] })
        assertEquals(sec("16:55"), second[0].arriveSec)
        assertEquals(sec("17:07:30"), second[1].departSec)
        // 구로는 갈아타는 데 4분이 걸린다
        assertTrue(second[1].departSec - second[0].arriveSec >= 240)
        assertEquals(sec("17:22"), result.plan.second!!.arriveSec)
    }

    @Test
    fun 평일_서울역에서_구로_30분_들렀다_21시까지_인천_급행으로_앞차를_따라잡는다() {
        val result = success(DayType.WEEKDAY, "서울역", "구로", "인천", arriveBy = sec("21:00"), stayMin = 30)

        // 구로에서 20:17:30 동인천행을 타고 동인천에서 먼저 떠난 인천행으로 갈아탄다.
        // 일반열차만 보던 예전 계산(20:11:30)보다 구로에서 6분 늦게 떠나도 된다
        val second = result.plan.second!!.rides
        assertEquals(sec("20:17:30"), second[0].departSec)
        assertEquals(listOf("동인천", "인천"), second.map { it.train.headsign })
        assertEquals(sec("20:59"), result.plan.second!!.arriveSec)
        assertEquals(sec("19:25:30"), result.plan.departSec)
    }

    @Test
    fun 휴일_서동탄에서_수원_1시간_들렀다_15시까지_서울역() {
        val result = success(DayType.HOLIDAY, "서동탄", "수원", "서울역", arriveBy = sec("15:00"), stayMin = 60)

        assertEquals(sec("12:28"), result.plan.departSec)
        assertEquals(sec("13:56:30"), result.plan.second!!.departSec)
        assertEquals(sec("14:49:30"), result.plan.second!!.arriveSec)
        assertEquals(sec("12:02"), result.alternative?.departSec)
    }

    @Test
    fun 평일_구로에서_부평_30분_들렀다_자정_넘어_00시30분까지_인천() {
        val result = success(DayType.WEEKDAY, "구로", "부평", "인천", arriveBy = sec("24:30"), stayMin = 30)

        assertEquals(sec("23:05:30"), result.plan.departSec)
        assertEquals(sec("23:59"), result.plan.second!!.departSec)
        assertEquals(sec("24:21"), result.plan.second!!.arriveSec)
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

    private fun calculate(day: DayType, from: String, via: String, to: String, arriveBy: Int, stayMin: Int): DepartureResult =
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
