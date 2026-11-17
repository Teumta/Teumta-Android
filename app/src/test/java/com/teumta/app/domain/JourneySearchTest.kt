package com.teumta.app.domain

import com.teumta.app.domain.model.DayType
import com.teumta.app.domain.model.Direction
import com.teumta.app.domain.model.Stop
import com.teumta.app.domain.model.Train
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/*갈아타기를 손으로 계산할 수 있게 만든 작은 노선. B에서 D 쪽으로 갈라진다(갈아타는 시간 3분).

  A ── B ── C
       └── D

열차  A 출발   B 도착/출발          C 도착
M1   08:00   08:10 / 08:10:30   08:20
M2   08:10   08:20 / 08:20:30   08:30

열차  B 출발   D 도착
S1   08:13   08:20
S2   08:23   08:30
S3   08:24   08:28   (B에서 갈아타기 3분이 모자라는 열차를 확인하려고 둔다)*/

class JourneySearchTest {

    private val trains = listOf(
        main("M1", "08:00"),
        main("M2", "08:10"),
        branch("S1", "08:13", "08:20"),
        branch("S2", "08:23", "08:30"),
        branch("S3", "08:24", "08:28"),
    )
    private val search = JourneySearch(trains) { stationId -> if (stationId == "B") 180 else 120 }

    @Test
    fun 갈아타는_시간만큼_여유가_있어야_다음_열차를_탄다() {
        // M2가 B에 08:20 도착 -> 3분 뒤 08:23인 S2는 탈 수 있고, 08:24 S3도 탈 수 있다
        assertEquals(listOf(sec("08:10"), sec("08:00")), search.latestDepartures("A", "D", sec("08:30"), limit = 2))
        // 08:29까지 도착하려면 S3(08:28 도착)를 타야 한다. M2 도착 08:20 + 3분 = 08:23 <= 08:24라 M2를 탈 수 있다
        assertEquals(listOf(sec("08:10")), search.latestDepartures("A", "D", sec("08:29"), limit = 1))
        // 08:27까지는 S1(08:20)만 된다. 그러려면 M1(B 08:10 도착 + 3분 = 08:13)
        assertEquals(listOf(sec("08:00")), search.latestDepartures("A", "D", sec("08:27"), limit = 1))
    }

    @Test
    fun 출발_시각을_정하면_가장_일찍_도착하는_열차를_고른다() {
        // A 08:10 출발이면 B에서 S2(08:30 도착)보다 S3(08:28 도착)가 이르다
        val journey = search.journey("A", "D", sec("08:10"), deadline = sec("08:30"))!!

        assertEquals(listOf("M2", "S3"), journey.rides.map { it.train.id })
        assertEquals("B", journey.rides[0].toId)
        assertEquals(sec("08:28"), journey.arriveSec)
    }

    @Test
    fun 같은_열차를_계속_타면_갈아타는_시간이_들지_않는다() {
        // B에 30초 서 있는 M1을 그대로 타고 C까지 간다
        val journey = search.journey("A", "C", sec("08:00"), deadline = sec("08:20"))!!
        assertEquals(listOf("M1"), journey.rides.map { it.train.id })
    }

    @Test
    fun 그_시각에_떠나는_열차가_없거나_마감에_못_닿으면_경로도_없다() {
        assertNull(search.journey("A", "D", sec("08:05"), deadline = sec("09:00")))
        assertNull(search.journey("A", "D", sec("08:10"), deadline = sec("08:27")))
        assertEquals(emptyList<Int>(), search.latestDepartures("A", "D", sec("08:19"), limit = 2))
    }

    // A 출발 시각만 받아 10분 간격으로 B, C를 만든다 (B에서 30초 선다)
    private fun main(id: String, departA: String): Train {
        val a = sec(departA)
        return train(id, listOf(Stop("A", a, a), Stop("B", a + 600, a + 630), Stop("C", a + 1200, a + 1200)))
    }

    private fun branch(id: String, departB: String, arriveD: String): Train =
        train(id, listOf(Stop("B", sec(departB), sec(departB)), Stop("D", sec(arriveD), sec(arriveD))))

    private fun train(id: String, stops: List<Stop>) = Train(id, DayType.WEEKDAY, Direction.DOWN, "행선지", stops)

    private fun sec(time: String): Int {
        val p = time.split(":").map { it.toInt() }
        return p[0] * 3600 + p[1] * 60 + p.getOrElse(2) { 0 }
    }
}
