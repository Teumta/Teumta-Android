package com.teumta.app.data

import com.teumta.app.domain.model.DayType
import com.teumta.app.domain.model.Direction
import com.teumta.app.domain.model.Stop
import com.teumta.app.domain.model.Train
import kotlinx.serialization.Serializable

// 서버 GET /api/trains?dayType= 의 응답 모양. 칸 이름은 서버의 TrainsResponse와 같다.
// 요일 종류가 열차마다가 아니라 바깥에 한 번만 있고, 열차 ID가 숫자라서 앱 모델(Train)과 모양이 다르다
@Serializable
data class TrainsResponse(
    // 시간표를 받은 날
    val baseDate: String,
    val dayType: DayType,
    val trains: List<TrainResponse>,
)

@Serializable
data class TrainResponse(
    val id: Long,
    val direction: Direction,
    val headsign: String,
    val stops: List<Stop>,
) {
    // 앱 모델로 바꾼다. 요일 종류는 바깥(TrainsResponse)에서 받아 열차마다 넣는다
    fun toTrain(dayType: DayType): Train = Train(
        id = id.toString(),
        dayType = dayType,
        direction = direction,
        headsign = headsign,
        stops = stops,
    )
}
