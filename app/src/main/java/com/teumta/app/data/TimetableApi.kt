package com.teumta.app.data

import com.teumta.app.domain.model.DayType
import com.teumta.app.domain.model.Station
import retrofit2.http.GET
import retrofit2.http.Query

// 시간표 서버(Teumta-Server)의 조회 API. Retrofit이 이 인터페이스를 보고 HTTP 요청 코드를 만든다.
// 응답 JSON은 앱 안 assets/line1 파일과 모양이 같아서 같은 모델(Station, TrainsResponse)로 읽는다
interface TimetableApi {

    // GET /api/stations -> 역 102개
    @GET("api/stations")
    suspend fun getStations(): List<Station>

    // GET /api/trains?dayType=WEEKDAY -> 평일(또는 휴일) 열차 전체
    @GET("api/trains")
    suspend fun getTrains(@Query("dayType") dayType: DayType): TrainsResponse
}
