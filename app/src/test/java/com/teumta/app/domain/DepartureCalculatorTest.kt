package com.teumta.app.domain

import com.teumta.app.domain.model.DayType
import com.teumta.app.domain.model.Direction
import com.teumta.app.domain.model.Station
import com.teumta.app.domain.model.Stop
import com.teumta.app.domain.model.Timetable
import com.teumta.app.domain.model.Train
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


/*손으로 계산할 수 있게 만든 작은 시간표로 확인한다.
역 A(1) -> B(2) -> C(3), 평일 인천 방면 열차 5편. B에서는 30초 서 있는다.

열차  A 출발   B 도착/출발          C 도착
T0   07:30   07:40 / 07:40:30   07:50
T1   08:00   08:10 / 08:10:30   08:20
T2   08:30   08:40 / 08:40:30   08:50
T3   09:00   09:10 / 09:10:30   09:20
T4   24:10   24:20 / 24:20:30   24:30  (자정 넘은 막차)*/

class DepartureCalculatorTest {

    private val timetable = Timetable(
        baseDate = "test",
        stations = listOf(Station("A", "A역", 1), Station("B", "B역", 2), Station("C", "C역", 3)),
        trains = listOf(
            train("T0", "07:30"),
            train("T1", "08:00"),
            train("T2", "08:30"),
            train("T3", "09:00"),
            train("T4", "24:10"),
        ),
    )
    private val calculator = DepartureCalculator(timetable)

    @Test
    fun 도착_시각에서_거꾸로_출발_열차와_대안을_찾는다() {
        // C에 09:00까지 -> B에서 T2(08:40:30) -> B 도착 마감 08:40:30 - 10분 - 2분 = 08:28:30 -> T1
        val result = calculate(arriveBy = "09:00", stayMin = 10)

        assertEquals("T1", result.plan.first.train.id)
        assertEquals(sec("08:00"), result.plan.departSec)
        assertEquals("T2", result.plan.second.train.id)
        assertEquals("T0", result.alternative?.first?.train?.id)
    }

    @Test
    fun 도착_시각과_정확히_같은_열차도_탈_수_있다() {
        assertEquals("T2", calculate(arriveBy = "08:50", stayMin = 10).plan.second.train.id)
        assertEquals("T1", calculate(arriveBy = "08:49:59", stayMin = 10).plan.second.train.id)
    }

    @Test
    fun 머무르는_시간이_0분이어도_내리고_타는_여유_2분은_둔다() {
        // B 도착 마감 08:40:30 - 2분 = 08:38:30 -> T2는 B에 08:40 도착이라 안 되고 T1
        val result = calculate(arriveBy = "09:00", stayMin = 0)

        assertEquals("T1", result.plan.first.train.id)
        // B에 08:10 도착, 08:40:30 출발 -> 여유 2분을 빼면 실제로 머무를 수 있는 시간은 28분 30초
        assertEquals(28 * 60 + 30, result.plan.staySec)
    }

    @Test
    fun 첫차보다_이른_도착_시각이면_실패() {
        assertFailure(FailReason.NO_TRAIN_BEFORE_ARRIVAL, calculator.calculate(request(arriveBy = "07:00", stayMin = 10)))
    }

    @Test
    fun 너무_오래_머무르면_실패() {
        // C 08:20 -> B에서 T1(08:10:30) -> B 도착 마감 07:08:30, 그 전에 B에 오는 열차 없음
        assertFailure(FailReason.STAY_TOO_LONG, calculator.calculate(request(arriveBy = "08:20", stayMin = 60)))
    }

    @Test
    fun 답_열차가_이미_떠났으면_실패() {
        val result = calculator.calculate(request(arriveBy = "09:00", stayMin = 10, now = "08:01"))
        assertFailure(FailReason.ALREADY_DEPARTED, result)
    }

    @Test
    fun 지금보다_먼저_떠난_대안은_보여_주지_않는다() {
        val result = calculator.calculate(request(arriveBy = "09:00", stayMin = 10, now = "07:45"))
        assertTrue(result is DepartureResult.Success)
        assertNull((result as DepartureResult.Success).alternative)
    }

    @Test
    fun 자정_넘은_도착_시각도_계산한다() {
        val result = calculate(arriveBy = "24:40", stayMin = 10)
        assertEquals("T4", result.plan.second.train.id)
        assertEquals("T3", result.plan.first.train.id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun 경유역이_출발역과_도착역_사이에_없으면_예외() {
        calculator.calculate(DepartureRequest("A", "C", "B", DayType.WEEKDAY, sec("09:00"), 10))
    }

    private fun calculate(arriveBy: String, stayMin: Int): DepartureResult.Success {
        val result = calculator.calculate(request(arriveBy, stayMin))
        assertTrue("성공해야 하는데 $result", result is DepartureResult.Success)
        return result as DepartureResult.Success
    }

    private fun request(arriveBy: String, stayMin: Int, now: String? = null) =
        DepartureRequest("A", "B", "C", DayType.WEEKDAY, sec(arriveBy), stayMin, now?.let(::sec))

    private fun assertFailure(reason: FailReason, result: DepartureResult) {
        assertEquals(DepartureResult.Failure(reason), result)
    }

    // A 출발 시각만 받아 10분 간격으로 B, C를 만든다
    private fun train(id: String, departA: String): Train {
        val a = sec(departA)
        return Train(
            id = id,
            dayType = DayType.WEEKDAY,
            direction = Direction.DOWN,
            headsign = "C",
            stops = listOf(Stop("A", a, a), Stop("B", a + 600, a + 630), Stop("C", a + 1200, a + 1200)),
        )
    }

    // "08:40:30", "24:10" -> 초
    private fun sec(time: String): Int {
        val p = time.split(":").map { it.toInt() }
        return p[0] * 3600 + p[1] * 60 + p.getOrElse(2) { 0 }
    }
}
