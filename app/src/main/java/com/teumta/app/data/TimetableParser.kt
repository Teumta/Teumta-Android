package com.teumta.app.data

import com.teumta.app.domain.model.Timetable
import kotlinx.serialization.json.Json

// JSON 문자열 -> Timetable. Android에 의존하지 않아서 단위 테스트에서도 쓴다
object TimetableParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): Timetable = json.decodeFromString(text)
}