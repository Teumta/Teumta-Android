package com.teumta.app.ui.result

import com.teumta.app.domain.DeparturePlan
import com.teumta.app.domain.Journey
import com.teumta.app.domain.Ride
import com.teumta.app.domain.model.DayType
import com.teumta.app.domain.model.Direction
import com.teumta.app.domain.model.Stop
import com.teumta.app.domain.model.Train
import com.teumta.app.ui.result.SegmentKind.STAY
import com.teumta.app.ui.result.SegmentKind.TRAIN
import com.teumta.app.ui.result.SegmentKind.TRANSFER
import com.teumta.app.ui.result.StopDot.END
import com.teumta.app.ui.result.StopDot.RIDE
import com.teumta.app.ui.result.StopDot.VIA
import org.junit.Assert.assertEquals
import org.junit.Test

/*역 A - B - C - D - E가 한 줄로 이어져 있다. 열차는 한 정거장에 5분씩 간다.
열차 X는 A에서 08:00, 열차 Y는 C에서 08:20, 열차 Z는 C에서 09:00에 떠난다*/

class ResultTimelineTest {

    private val names = mapOf("A" to "가역", "B" to "나역", "C" to "다역", "D" to "라역", "E" to "마역")
    private val nameOf: (String) -> String = { names.getValue(it) }

    private val x = train("X", "A", "08:00", "마역")
    private val y = train("Y", "C", "08:20", "마역")
    private val z = train("Z", "C", "09:00", "마역")

    @Test
    fun 바로_가면_승차_레일_도착() {
        // A 08:00 승차 → E 08:20 도착(4정거장), 목표 08:30
        val plan = DeparturePlan(Journey(listOf(ride(x, "A", "E"))))

        assertEquals(
            listOf(
                TimelineItem.Stop(sec("08:00"), "가역 승차", "마역행", RIDE, RealtimeTarget("A", "E")),
                TimelineItem.Segment("20분 · 4개 역", TRAIN),
                TimelineItem.Stop(sec("08:20"), "마역 도착", "목표보다 10분 일찍", END),
            ),
            buildTimeline(plan, nameOf, arriveBySec = sec("08:30")),
        )
        assertEquals(
            listOf(Fact("걸리는 시간", "20분"), Fact("마역 도착", "08:20"), Fact("목표", "08:30")),
            buildFacts(plan, "마역", sec("08:30")),
        )
    }

    @Test
    fun 갈아타면_하차와_갈아타기_줄이_낀다() {
        // A 08:00 X → C 08:10 하차, 10분 기다려 C 08:20 Y → E 08:30
        val plan = DeparturePlan(Journey(listOf(ride(x, "A", "C"), ride(y, "C", "E"))))

        assertEquals(
            listOf(
                TimelineItem.Stop(sec("08:00"), "가역 승차", "마역행", RIDE, RealtimeTarget("A", "C")),
                TimelineItem.Segment("10분 · 2개 역", TRAIN),
                TimelineItem.Stop(sec("08:10"), "다역 하차", null, RIDE),
                TimelineItem.Segment("다역 갈아타기 · 10분", TRANSFER),
                TimelineItem.Stop(sec("08:20"), "다역 승차", "마역행", RIDE, RealtimeTarget("C", "E")),
                TimelineItem.Segment("10분 · 2개 역", TRAIN),
                TimelineItem.Stop(sec("08:30"), "마역 도착", "목표 시각에 맞춰 도착", END),
            ),
            buildTimeline(plan, nameOf, arriveBySec = sec("08:30")),
        )
    }

    @Test
    fun 들르면_들를_역_하차_머무름_승차가_주황으로_낀다() {
        // A 08:00 X → C 08:10 하차, C 09:00 Z 승차 → E 09:10. 머무름 = 50분 - 여유 2분 = 48분
        val plan = DeparturePlan(Journey(listOf(ride(x, "A", "C"))), Journey(listOf(ride(z, "C", "E"))))

        assertEquals(
            listOf(
                TimelineItem.Stop(sec("08:00"), "가역 승차", "마역행", RIDE, RealtimeTarget("A", "C")),
                TimelineItem.Segment("10분 · 2개 역", TRAIN),
                TimelineItem.Stop(sec("08:10"), "다역 하차", null, VIA),
                TimelineItem.Segment("48분 머무름 · 내리고 타는 여유 2분 제외", STAY),
                TimelineItem.Stop(sec("09:00"), "다역 승차", "마역행", VIA, RealtimeTarget("C", "E")),
                TimelineItem.Segment("10분 · 2개 역", TRAIN),
                TimelineItem.Stop(sec("09:10"), "마역 도착", "목표보다 20분 일찍", END),
            ),
            buildTimeline(plan, nameOf, arriveBySec = sec("09:30")),
        )
        assertEquals(Fact("머무는 시간", "48분"), buildFacts(plan, "마역", sec("09:30")).first())
    }

    @Test
    fun 한_대_앞_열차_안내() {
        val direct = DeparturePlan(Journey(listOf(ride(x, "A", "E"))))
        assertEquals("한 대 앞 08:00 열차를 타면 08:20에 도착해요", alternativeText(direct))

        val withVia = DeparturePlan(Journey(listOf(ride(x, "A", "C"))), Journey(listOf(ride(z, "C", "E"))))
        assertEquals("한 대 앞 08:00 열차를 타면 48분 머물러요", alternativeText(withVia))
    }

    // startId에서 start에 떠나 E까지 한 정거장에 5분씩 가는 열차
    private fun train(id: String, startId: String, start: String, headsign: String): Train {
        val ids = listOf("A", "B", "C", "D", "E").dropWhile { it != startId }
        return Train(
            id = id,
            dayType = DayType.WEEKDAY,
            direction = Direction.DOWN,
            headsign = headsign,
            stops = ids.mapIndexed { i, s -> Stop(s, sec(start) + i * 300, sec(start) + i * 300) },
        )
    }

    private fun ride(train: Train, fromId: String, toId: String): Ride {
        val from = train.stops.first { it.stationId == fromId }
        val to = train.stops.first { it.stationId == toId }
        return Ride(train, fromId, from.dep, toId, to.arr)
    }

    private fun sec(time: String): Int {
        val (h, m) = time.split(":").map(String::toInt)
        return h * 3600 + m * 60
    }
}
