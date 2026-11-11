package com.teumta.app.domain.model

import kotlinx.serialization.Serializable

// assets/timetable.json 전체. 필드 이름이 JSON 키와 같아서 그대로 읽힌다
@Serializable
data class Timetable(
    val baseDate: String,
    val stations: List<Station>,
    val trains: List<Train>,
)

@Serializable
data class Station(
    val id: String,
    val name: String,
    // 서울역 1 ~ 인천 29
    val order: Int,
)

@Serializable
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

// DOWN: 인천 방면, UP: 서울역 방면
@Serializable
enum class Direction { DOWN, UP }