package com.teumta.app.data.realtime

import retrofit2.http.GET
import retrofit2.http.Query

// 틈타 서버의 실시간 API. 서버가 서울 열린데이터광장의 1호선 열차 위치를 90초에 한 번 받아 역마다 정리해 준다
interface RealtimeApi {

    // GET /api/realtime/arrivals?stationId=탈 역&towardStationId=내릴 역 -> 탈 역으로 오는 열차(가까운 순서).
    // 내릴 역을 주면 그 역까지 가는 열차만 온다. 인증키가 없거나 서울시가 안 되면 503
    @GET("api/realtime/arrivals")
    suspend fun getArrivals(
        @Query("stationId") stationId: String,
        @Query("towardStationId") towardStationId: String,
    ): RealtimeArrivalsResponse
}
