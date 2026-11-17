package com.teumta.app.data

import com.teumta.app.domain.model.Station
import com.teumta.app.domain.model.Timetable
import kotlinx.serialization.json.Json

// JSON 문자열 -> 시간표. 서버 응답 형식(GET /api/stations, GET /api/trains?dayType=)을 읽는다.
// Android에 의존하지 않아서 단위 테스트에서도 쓴다
object TimetableParser {
    // 서버가 칸을 더 보내도(예: 역의 branch) 앱이 모르는 칸은 건너뛴다
    private val json = Json { ignoreUnknownKeys = true }

    // 역은 칸 이름이 앱 모델(Station)과 같아서 바로 읽는다
    fun parseStations(text: String): List<Station> = json.decodeFromString(text)

    // 평일 또는 휴일 열차 하나. 열차는 모양이 달라서 TrainsResponse로 읽고 combine에서 바꾼다
    fun parseTrains(text: String): TrainsResponse = json.decodeFromString(text)

    // 역과 평일·휴일 열차를 Timetable 하나로 합친다
    fun combine(stations: List<Station>, days: List<TrainsResponse>): Timetable {
        // 평일과 휴일 시간표를 받은 날이 다르면 서로 다른 시간표가 섞인 것이다
        require(days.map { it.baseDate }.distinct().size == 1) { "시간표 기준일이 서로 다르다: ${days.map { it.baseDate }}" }
        return Timetable(
            baseDate = days.first().baseDate,
            stations = stations,
            trains = days.flatMap { day -> day.trains.map { it.toTrain(day.dayType) } },
        )
    }

    // 문자열 세 개를 한 번에 읽어 합친다 (테스트용)
    fun parseServer(stationsText: String, trainsTexts: List<String>): Timetable =
        combine(parseStations(stationsText), trainsTexts.map(::parseTrains))
}
