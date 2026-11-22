package com.teumta.app.domain.model

import java.time.Instant

// 열차가 지금 있는 역(currentStation)에서 어떤 상태인지
enum class TrainPosition {
    ENTERING,       // 그 역에 들어가는 중
    ARRIVED,        // 그 역에 서 있다
    DEPARTED,       // 그 역을 막 떠났다
    PREV_DEPARTED,  // 그 역의 전 역을 떠나 그 역으로 가는 중
}

// 탈 역으로 오는 열차 한 편(서울시 실시간 열차 위치)
data class RealtimeArrival(
    val trainNo: String,
    // 행선지("인천행"의 인천)
    val terminal: String,
    val express: Boolean,
    val currentStation: String,
    val position: TrainPosition,
    // currentStation에서 탈 역까지 몇 역(탈 역에 있으면 0)
    val stationsAway: Int,
    // 탈 역이 이 열차의 끝 역이라 도착한 뒤 돌아 나간다(광명 셔틀, 서동탄 등)
    val turnsBack: Boolean,
    // 서울시가 이 위치를 받은 시각. 길게는 5분 전 것까지 온다
    val receivedAt: Instant,
)
