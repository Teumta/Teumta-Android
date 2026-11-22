package com.teumta.app.ui.result

import com.teumta.app.domain.CalculationDay
import com.teumta.app.domain.DeparturePlan
import com.teumta.app.domain.DepartureResult
import com.teumta.app.domain.FailReason
import com.teumta.app.domain.Journey
import com.teumta.app.domain.Ride
import com.teumta.app.domain.model.DayType
import com.teumta.app.domain.model.Direction
import com.teumta.app.domain.model.RealtimeArrival
import com.teumta.app.domain.model.Stop
import com.teumta.app.domain.model.TimetableSource
import com.teumta.app.domain.model.Train
import com.teumta.app.domain.model.TrainPosition
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class RealtimeTextTest {

    @Test
    fun 역_이름에_로_으로를_알맞게_붙인다() {
        assertEquals("구로로", withRo("구로"))           // 받침 없음
        assertEquals("신도림으로", withRo("신도림"))      // 받침 ㅁ
        assertEquals("서울역으로", withRo("서울역"))      // 받침 ㄱ
        assertEquals("금천구청으로", withRo("금천구청"))  // 받침 ㅇ
        assertEquals("병점으로", withRo("병점"))
        assertEquals("오류동으로", withRo("오류동"))
        assertEquals("부평으로", withRo("부평"))
        assertEquals("광명으로", withRo("광명"))
        assertEquals("서동탄으로", withRo("서동탄"))
        assertEquals("지행으로", withRo("지행"))
        assertEquals("온양온천으로", withRo("온양온천"))
        assertEquals("제물포로", withRo("제물포"))
        assertEquals("가능으로", withRo("가능"))
    }

    @Test
    fun ㄹ_받침은_로() {
        // 1호선에는 ㄹ 받침으로 끝나는 역이 없어서 만든 이름으로 확인한다
        assertEquals("서울로", withRo("서울"))
    }

    @Test
    fun 탈_역에_있으면_당역() {
        assertEquals("당역 도착", realtimePositionText(arrival(0, TrainPosition.ARRIVED)))
        assertEquals("당역 진입", realtimePositionText(arrival(0, TrainPosition.ENTERING)))
        assertEquals("당역 출발", realtimePositionText(arrival(0, TrainPosition.DEPARTED)))
        // 전 역을 떠나 탈 역으로 오는 중
        assertEquals("전역 출발", realtimePositionText(arrival(0, TrainPosition.PREV_DEPARTED)))
    }

    @Test
    fun 바로_전_역에_있으면_전역() {
        assertEquals("전역 도착", realtimePositionText(arrival(1, TrainPosition.ARRIVED, "신도림")))
        assertEquals("전역 진입", realtimePositionText(arrival(1, TrainPosition.ENTERING, "신도림")))
        assertEquals("전역 출발", realtimePositionText(arrival(1, TrainPosition.DEPARTED, "신도림")))
        assertEquals("전전역 출발", realtimePositionText(arrival(1, TrainPosition.PREV_DEPARTED, "신도림")))
    }

    @Test
    fun 더_멀면_몇_역_전과_지금_역을_쓴다() {
        assertEquals("2역 전 · 영등포 도착", realtimePositionText(arrival(2, TrainPosition.ARRIVED, "영등포")))
        assertEquals("3역 전 · 신길 출발", realtimePositionText(arrival(3, TrainPosition.DEPARTED, "신길")))
        assertEquals("5역 전 · 노량진으로 가는 중", realtimePositionText(arrival(5, TrainPosition.PREV_DEPARTED, "노량진")))
    }

    @Test
    fun 몇_분_전_위치인지() {
        val now = Instant.parse("2026-11-21T14:00:20Z")
        assertEquals("방금 위치", realtimeAgeText(now.minusSeconds(59), now))
        assertEquals("1분 전 위치", realtimeAgeText(now.minusSeconds(60), now))
        assertEquals("2분 전 위치", realtimeAgeText(now.minusSeconds(136), now))
    }

    @Test
    fun 오늘_계산이면_타는_역마다_그_열차에서_내릴_역으로_묻는다() {
        // A 08:00 X → B 갈아타기, B 08:15 Y → C. 지금 07:30이면 두 곳 다 1시간 안
        assertEquals(
            listOf(RealtimeTarget("A", "B"), RealtimeTarget("B", "C")),
            realtimeTargets(ready(nowSec = sec("07:30"))),
        )
    }

    @Test
    fun 타는_시각이_1시간_넘게_남은_역은_뺀다() {
        // 07:10이면 A(08:00)는 50분 뒤라 넣고, B(08:15)는 65분 뒤라 뺀다
        assertEquals(listOf(RealtimeTarget("A", "B")), realtimeTargets(ready(nowSec = sec("07:10"))))
        // 07:00이면 A가 딱 1시간이라 넣는다
        assertEquals(listOf(RealtimeTarget("A", "B")), realtimeTargets(ready(nowSec = sec("07:00"))))
        assertEquals(emptyList<RealtimeTarget>(), realtimeTargets(ready(nowSec = sec("06:59"))))
    }

    @Test
    fun 다른_날이거나_답이_없으면_묻지_않는다() {
        assertEquals(emptyList<RealtimeTarget>(), realtimeTargets(ready(nowSec = null)))
        assertEquals(
            emptyList<RealtimeTarget>(),
            realtimeTargets(ready(nowSec = sec("07:30")).copy(result = DepartureResult.Failure(FailReason.ALREADY_DEPARTED))),
        )
    }

    private fun arrival(away: Int, position: TrainPosition, station: String = "구로") = RealtimeArrival(
        trainNo = "0165",
        terminal = "인천",
        express = false,
        currentStation = station,
        position = position,
        stationsAway = away,
        turnsBack = false,
        receivedAt = Instant.EPOCH,
    )

    private fun ready(nowSec: Int?): ResultUiState.Ready {
        val first = train("X", listOf("A" to "08:00", "B" to "08:10"))
        val second = train("Y", listOf("B" to "08:15", "C" to "08:30"))
        val plan = DeparturePlan(
            Journey(listOf(Ride(first, "A", sec("08:00"), "B", sec("08:10")), Ride(second, "B", sec("08:15"), "C", sec("08:30")))),
        )
        return ResultUiState.Ready(
            day = CalculationDay(LocalDate.of(2026, 11, 21), DayType.HOLIDAY, nowSec),
            result = DepartureResult.Success(plan, alternative = null),
            stationNames = mapOf("A" to "가역", "B" to "나역", "C" to "다역"),
            fromId = "A",
            viaId = null,
            toId = "C",
            arriveBySec = sec("09:00"),
            stayMin = 0,
            timetableSource = TimetableSource.SERVER,
            baseDate = "2026-11-06",
        )
    }

    private fun train(id: String, stops: List<Pair<String, String>>) = Train(
        id = id,
        dayType = DayType.HOLIDAY,
        direction = Direction.DOWN,
        headsign = "다역",
        stops = stops.map { (s, t) -> Stop(s, sec(t), sec(t)) },
    )

    private fun sec(time: String): Int {
        val (h, m) = time.split(":").map(String::toInt)
        return h * 3600 + m * 60
    }
}
