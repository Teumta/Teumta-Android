package com.teumta.app.ui.input

import com.teumta.app.domain.ServiceTime
import com.teumta.app.domain.model.Station
import java.time.LocalTime
import java.time.ZonedDateTime

data class InputUiState(
    val isLoading: Boolean = true,
    val stations: List<Station> = emptyList(),
    val selection: RouteSelection = RouteSelection(),
    val arriveBy: LocalTime,
    val stayMin: Int = 30,
) {
    val canCalculate: Boolean get() = selection.isComplete

    fun toRouteInput(): RouteInput? {
        val s = selection
        if (!s.isComplete) return null
        return RouteInput(s.from!!.id, s.via!!.id, s.to!!.id, arriveBy, stayMin)
    }
}

// 계산 버튼을 눌렀을 때 결과 화면으로 넘길 값
data class RouteInput(
    val fromId: String,
    val viaId: String,
    val toId: String,
    val arriveBy: LocalTime,
    val stayMin: Int,
)

// 처음 보여 줄 도착 시각: 지금(한국 시각)부터 1시간 30분 뒤를 10분 단위로 올린다.
// 열차가 없는 새벽(01~06시)이면 09:00으로 둔다. 언제 열어도 결과가 나오게 하려는 것이다
fun defaultArriveBy(now: ZonedDateTime): LocalTime {
    val later = now.withZoneSameInstant(ServiceTime.SEOUL).toLocalTime().plusMinutes(90)
    val roundUp = (later.minute + 9) / 10 * 10
    val rounded = LocalTime.of(later.hour, 0).plusMinutes(roundUp.toLong())
    return if (rounded.hour in 1..6) LocalTime.of(9, 0) else rounded
}
