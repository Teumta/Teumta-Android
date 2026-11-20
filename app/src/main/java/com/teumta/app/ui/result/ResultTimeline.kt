package com.teumta.app.ui.result

import com.teumta.app.domain.DepartureCalculator
import com.teumta.app.domain.DeparturePlan
import com.teumta.app.domain.Journey
import com.teumta.app.domain.Ride
import com.teumta.app.ui.formatClock
import com.teumta.app.ui.formatDuration

// 결과 화면에 보여 줄 글을 계산 결과에서 만든다. 화면 코드 없이 테스트할 수 있게 따로 둔다

// 동선 한 줄. 역에 서는 줄(Stop)과, 두 줄 사이를 잇는 레일 줄(Segment)이 번갈아 나온다
sealed interface TimelineItem {
    data class Stop(val timeSec: Int, val title: String, val detail: String?, val dot: StopDot) : TimelineItem
    data class Segment(val label: String, val kind: SegmentKind) : TimelineItem
}

enum class StopDot {
    // 1호선 파란 고리: 열차를 타고 내리는 역
    RIDE,

    // 주황 고리: 들를 역
    VIA,

    // 남색 채운 점: 도착역
    END,
}

enum class SegmentKind {
    // 열차를 타고 가는 구간 (파란 레일)
    TRAIN,

    // 들를 역에서 머무는 구간 (주황 레일, 주황 글자)
    STAY,

    // 같은 역에서 다른 열차로 갈아타며 기다리는 구간 (회색 점선 레일)
    TRANSFER,
}

// 답 블록 아래 세 칸
data class Fact(val label: String, val value: String)

// 출발역 승차부터 도착역 도착까지. 들르면 들를 역 하차 → 머무름 → 들를 역 승차가 끼고, 갈아타면 하차 → 갈아타기 → 승차가 낀다
fun buildTimeline(plan: DeparturePlan, nameOf: (String) -> String, arriveBySec: Int): List<TimelineItem> {
    val arrival = TimelineItem.Stop(plan.arriveSec, "${nameOf((plan.second ?: plan.first).toId)} 도착", earlyText(arriveBySec - plan.arriveSec), StopDot.END)
    val second = plan.second
    val staySec = plan.staySec
    if (second == null || staySec == null) {
        return journeyItems(plan.first, nameOf, firstDot = StopDot.RIDE, last = arrival)
    }
    val via = nameOf(plan.first.toId)
    return journeyItems(plan.first, nameOf, StopDot.RIDE, TimelineItem.Stop(plan.first.arriveSec, "$via 하차", null, StopDot.VIA)) +
        TimelineItem.Segment(
            "${formatDuration(staySec)} 머무름 · 내리고 타는 여유 ${DepartureCalculator.TRANSFER_BUFFER_SEC / 60}분 제외",
            SegmentKind.STAY,
        ) +
        journeyItems(second, nameOf, firstDot = StopDot.VIA, last = arrival)
}

// 열차 한 편마다 승차 → 레일. 갈아타면 그 역 하차 → 갈아타기가 끼고, 마지막 열차는 last로 끝난다
private fun journeyItems(journey: Journey, nameOf: (String) -> String, firstDot: StopDot, last: TimelineItem.Stop): List<TimelineItem> =
    journey.rides.flatMapIndexed { i, ride ->
        val board = TimelineItem.Stop(
            timeSec = ride.departSec,
            title = "${nameOf(ride.fromId)} 승차",
            detail = "${ride.train.headsign}행",
            dot = if (i == 0) firstDot else StopDot.RIDE,
        )
        val rail = TimelineItem.Segment("${formatDuration(ride.arriveSec - ride.departSec)} · ${stationCount(ride)}개 역", SegmentKind.TRAIN)
        val next = journey.rides.getOrNull(i + 1)
        if (next == null) {
            listOf(board, rail, last.copy(timeSec = ride.arriveSec))
        } else {
            val transfer = nameOf(ride.toId)
            listOf(
                board,
                rail,
                TimelineItem.Stop(ride.arriveSec, "$transfer 하차", null, StopDot.RIDE),
                TimelineItem.Segment("$transfer 갈아타기 · ${formatDuration(next.departSec - ride.arriveSec)}", SegmentKind.TRANSFER),
            )
        }
    }

// 이 열차로 몇 정거장 가는지
private fun stationCount(ride: Ride): Int {
    val ids = ride.train.stops.map { it.stationId }
    return ids.indexOf(ride.toId) - ids.indexOf(ride.fromId)
}

// 도착역 줄 밑: 목표보다 얼마나 일찍 닿는지
private fun earlyText(spareSec: Int): String =
    if (spareSec >= 60) "목표보다 ${formatDuration(spareSec)} 일찍" else "목표 시각에 맞춰 도착"

// 답 블록 아래 세 칸. 들르면 머무는 시간, 바로 가면 걸리는 시간이 첫 칸이다
fun buildFacts(plan: DeparturePlan, toName: String, arriveBySec: Int): List<Fact> {
    val first = plan.staySec?.let { Fact("머무는 시간", formatDuration(it)) }
        ?: Fact("걸리는 시간", formatDuration(plan.arriveSec - plan.departSec))
    return listOf(first, Fact("$toName 도착", formatClock(plan.arriveSec)), Fact("목표", formatClock(arriveBySec)))
}

// 한 대 앞 열차 한 줄. 들르면 더 머무를 수 있는 시간, 바로 가면 도착 시각
fun alternativeText(alternative: DeparturePlan): String {
    val head = "한 대 앞 ${formatClock(alternative.departSec)} 열차를 타면"
    return alternative.staySec?.let { "$head ${formatDuration(it)} 머물러요" }
        ?: "$head ${formatClock(alternative.arriveSec)}에 도착해요"
}
