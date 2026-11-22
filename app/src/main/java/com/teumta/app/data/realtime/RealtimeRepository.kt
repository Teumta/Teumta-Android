package com.teumta.app.data.realtime

import com.teumta.app.domain.model.RealtimeArrival
import com.teumta.app.domain.model.TrainPosition
import retrofit2.HttpException
import java.io.IOException
import java.time.DateTimeException
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

// 탈 역으로 오는 열차를 서버에서 받는다. 실시간은 있으면 좋은 정보라, 못 받으면 오류를 띄우지 않고 null을 준다.
// 화면은 null이면 실시간 줄만 숨기고 시간표 계산은 그대로 보여 준다
@Singleton
class RealtimeRepository @Inject constructor(private val api: RealtimeApi) {

    suspend fun getArrivals(stationId: String, towardStationId: String): List<RealtimeArrival>? = try {
        api.getArrivals(stationId, towardStationId).arrivals.mapNotNull { it.toModel() }
    } catch (e: IOException) {
        null // 서버가 꺼져 있음, 인터넷 없음, 시간 초과
    } catch (e: HttpException) {
        null // 503(인증키 없음, 서울시 안 됨, 하루 한도) 같은 오류 응답
    } catch (e: IllegalArgumentException) {
        null // JSON 모양이 다름(SerializationException)
    }
}

// 서버 한 줄 -> 앱 모델. 모르는 상태나 읽을 수 없는 시각이면 그 줄만 버린다(null)
internal fun RealtimeArrivalResponse.toModel(): RealtimeArrival? {
    val position = TrainPosition.entries.firstOrNull { it.name == currentStatus } ?: return null
    val received = try {
        OffsetDateTime.parse(receivedAt).toInstant()
    } catch (e: DateTimeException) {
        return null
    }
    return RealtimeArrival(trainNo, terminal, express, currentStation, position, stationsAway, turnsBack, received)
}
