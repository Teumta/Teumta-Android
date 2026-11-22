package com.teumta.app.data.realtime

import kotlinx.serialization.Serializable

// 서버 GET /api/realtime/arrivals 의 응답 모양. 칸 이름은 서버의 RealtimeArrivalsResponse와 같다.
// 상태는 글자로 받는다. 서버에 새 상태가 생겨도 응답 전체를 못 읽는 일이 없게, 모르는 값은 그 줄만 버린다
@Serializable
data class RealtimeArrivalsResponse(
    val stationId: String,
    val fetchedAt: String,
    val arrivals: List<RealtimeArrivalResponse>,
)

@Serializable
data class RealtimeArrivalResponse(
    val trainNo: String,
    val terminal: String,
    val express: Boolean,
    val currentStation: String,
    // ENTERING, ARRIVED, DEPARTED, PREV_DEPARTED
    val currentStatus: String,
    val stationsAway: Int,
    val turnsBack: Boolean,
    // "2026-11-21T19:27:33+09:00"
    val receivedAt: String,
)
