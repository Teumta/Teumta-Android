package com.teumta.app.data

import com.teumta.app.domain.model.Station
import com.teumta.app.domain.model.Timetable
import kotlinx.serialization.json.Json

// JSON 문자열 -> Timetable. Android에 의존하지 않아서 단위 테스트에서도 쓴다
object TimetableParser {
    // 서버가 칸을 더 보내도(역의 branch) 앱이 모르는 칸은 건너뛴다
    private val json = Json { ignoreUnknownKeys = true }

    // 지금 앱 안 timetable.json(서울역~인천) 형식
    fun parse(text: String): Timetable = json.decodeFromString(text)

    // 서버 응답 형식. GET /api/stations 하나와 GET /api/trains?dayType= 평일·휴일 응답을 Timetable 하나로 합친다.
    // 역은 칸 이름이 앱 모델(Station)과 같아서 바로 읽고, 열차는 TrainsResponse를 거쳐 바꾼다
    fun parseServer(stationsText: String, trainsTexts: List<String>): Timetable {
        val stations = json.decodeFromString<List<Station>>(stationsText)
        val days = trainsTexts.map { json.decodeFromString<TrainsResponse>(it) }
        // 평일과 휴일 시간표를 받은 날이 다르면 서로 다른 시간표가 섞인 것이다
        require(days.map { it.baseDate }.distinct().size == 1) { "시간표 기준일이 서로 다르다: ${days.map { it.baseDate }}" }
        return Timetable(
            baseDate = days.first().baseDate,
            stations = stations,
            trains = days.flatMap { day -> day.trains.map { it.toTrain(day.dayType) } },
        )
    }
}
