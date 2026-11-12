package com.teumta.app.data

import com.teumta.app.domain.model.Timetable

// 시간표를 어디서 가져오는지 숨긴다.
// 지금은 앱 안 JSON(LocalTimetableRepository), Day 4부터 서버(RemoteTimetableRepository)를 붙인다.
interface TimetableRepository {
    suspend fun getTimetable(): Timetable
}