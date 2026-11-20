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

        assertEquals(listOf("T1"), result.plan.first.trainIds)
        assertEquals(sec("08:00"), result.plan.departSec)
        assertEquals(listOf("T2"), result.plan.second!!.trainIds)
        assertEquals(listOf("T0"), result.alternative?.first?.trainIds)
    }

    @Test
    fun 도착_시각과_정확히_같은_열차도_탈_수_있다() {
        assertEquals(listOf("T2"), calculate(arriveBy = "08:50", stayMin = 10).plan.second!!.trainIds)
        assertEquals(listOf("T1"), calculate(arriveBy = "08:49:59", stayMin = 10).plan.second!!.trainIds)
    }

    @Test
    fun 머무르는_시간이_0분이어도_내리고_타는_여유_2분은_둔다() {
        // B 도착 마감 08:40:30 - 2분 = 08:38:30 -> T2는 B에 08:40 도착이라 안 되고 T1
        val result = calculate(arriveBy = "09:00", stayMin = 0)

        assertEquals(listOf("T1"), result.plan.first.trainIds)
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
    fun 너무_오래_머무르면_가장_오래_머무를_수_있는_시간을_알려_준다() {
        // C 08:20 -> B에서 T1(08:10:30). A에서 가장 일찍 떠나는 T0이 B에 07:40 도착
        // -> 08:10:30 - 07:40 - 2분 = 28분 30초
        assertEquals(28 * 60 + 30, calculator.maxStaySec(request(arriveBy = "08:20", stayMin = 60)))
        // 지금이 07:31이면 T0은 떠났고, 그다음 T1은 B에 08:10에 닿아 08:10:30 열차를 탈 여유(2분)가 없다
        assertNull(calculator.maxStaySec(request(arriveBy = "08:20", stayMin = 60, now = "07:31")))
        // 들를 역이 없으면 머무를 시간도 없다
        assertNull(calculator.maxStaySec(directRequest(arriveBy = "08:20")))
    }

    @Test
    fun 지금_떠나는_열차로_덜_머물면_들를_수_있으면_이미_떠났다가_아니라_머무를_시간이_문제다() {
        // C 09:20 -> B에서 T3(09:10:30) -> 60분 머물려면 A에서 T0(07:30)을 타야 하는데 지금은 08:01.
        // 지금 탈 수 있는 T2(B 08:40)로 가면 28분 30초는 머무를 수 있다
        val request = request(arriveBy = "09:20", stayMin = 60, now = "08:01")
        assertFailure(FailReason.STAY_TOO_LONG, calculator.calculate(request))
        assertEquals(28 * 60 + 30, calculator.maxStaySec(request))
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
        assertEquals(listOf("T4"), result.plan.second!!.trainIds)
        assertEquals(listOf("T3"), result.plan.first.trainIds)
    }

    @Test
    fun 들를_역이_없으면_도착역까지_바로_가는_가장_늦은_열차() {
        // C에 09:00까지 -> T2(C 08:50 도착)가 답, T1이 대안. 머무름이 없어 staySec은 null
        val result = calculator.calculate(directRequest(arriveBy = "09:00")) as DepartureResult.Success

        assertEquals(listOf("T2"), result.plan.first.trainIds)
        assertNull(result.plan.second)
        assertNull(result.plan.staySec)
        assertEquals(sec("08:30"), result.plan.departSec)
        assertEquals(sec("08:50"), result.plan.arriveSec)
        assertEquals(listOf("T1"), result.alternative?.first?.trainIds)
    }

    @Test
    fun 바로_가기도_첫차_전이면_실패하고_이미_떠났으면_실패() {
        assertFailure(FailReason.NO_TRAIN_BEFORE_ARRIVAL, calculator.calculate(directRequest(arriveBy = "07:49")))
        assertFailure(FailReason.ALREADY_DEPARTED, calculator.calculate(directRequest(arriveBy = "09:00", now = "08:31")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun 같은_역을_두_번_고르면_예외() {
        calculator.calculate(DepartureRequest("A", "A", "C", DayType.WEEKDAY, sec("09:00"), 10))
    }

    private fun calculate(arriveBy: String, stayMin: Int): DepartureResult.Success {
        val result = calculator.calculate(request(arriveBy, stayMin))
        assertTrue("성공해야 하는데 $result", result is DepartureResult.Success)
        return result as DepartureResult.Success
    }

    private fun request(arriveBy: String, stayMin: Int, now: String? = null) =
        DepartureRequest("A", "B", "C", DayType.WEEKDAY, sec(arriveBy), stayMin, now?.let(::sec))

    private fun directRequest(arriveBy: String, now: String? = null) =
        DepartureRequest("A", null, "C", DayType.WEEKDAY, sec(arriveBy), stayMin = 0, nowSec = now?.let(::sec))

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

    // 탄 열차 ID를 순서대로. 갈아타지 않으면 하나다
    private val Journey.trainIds: List<String> get() = rides.map { it.train.id }

    // "08:40:30", "24:10" -> 초
    private fun sec(time: String): Int {
        val p = time.split(":").map { it.toInt() }
        return p[0] * 3600 + p[1] * 60 + p.getOrElse(2) { 0 }
    }
}
