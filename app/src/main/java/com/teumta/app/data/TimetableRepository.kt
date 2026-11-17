package com.teumta.app.data

import com.teumta.app.domain.model.Station
import com.teumta.app.domain.model.Timetable

// 시간표를 어디서 가져오는지 숨긴다.
// 지금은 앱 안 JSON(LocalTimetableRepository), 다음에 서버(RemoteTimetableRepository)를 붙인다.
interface TimetableRepository {
    // 역 목록만. 입력 화면은 역만 있으면 되고, 파일이 작아서 금방 읽는다
    suspend fun getStations(): List<Station>

    // 역과 평일·휴일 열차 전체. 계산할 때 쓴다
    suspend fun getTimetable(): Timetable
}
