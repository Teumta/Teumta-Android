package com.teumta.app.domain.model

import kotlinx.serialization.Serializable

// 계산에 쓰는 시간표 전체. TimetableParser가 서버 응답 모양(역, 평일·휴일 열차)을 합쳐 만든다
data class Timetable(
    val baseDate: String,
    val stations: List<Station>,
    val trains: List<Train>,
)

// 서버 GET /api/stations의 칸 이름과 같아서 그대로 읽힌다
@Serializable
data class Station(
    val id: String,
    val name: String,
    // 연천 1 ~ 서동탄 102. 연천에서부터 갈래를 하나씩 훑은 순서라, 다른 갈래의 역끼리는 순서로 앞뒤를 알 수 없다
    val order: Int,
    // 연천 쪽으로 이웃한 역. 갈라지는 곳에서는 갈라져 나온 역을 가리킨다 (예: 광명 -> 금천구청, 구일 -> 구로). 연천은 null
    val parentId: String? = null,
    // 이 역에서 다른 열차로 갈아탈 때 걸리는 시간(초). 없으면 2분으로 본다 (예: 구로 240초)
    val transferSec: Int? = null,
)

data class Train(
    val id: String,
    val dayType: DayType,
    val direction: Direction,
    // 행선지 (OO행)
    val headsign: String,
    val stops: List<Stop>,
)

// 시각은 운행일 0시 기준 초. 자정이 넘으면 86400 이상이다
@Serializable
data class Stop(
    val stationId: String,
    val arr: Int,
    val dep: Int,
)

@Serializable
enum class DayType { WEEKDAY, HOLIDAY }

// DOWN: 연천에서 멀어지는 쪽(인천·신창·광명·서동탄 방면), UP: 연천 쪽
@Serializable
enum class Direction { DOWN, UP }
