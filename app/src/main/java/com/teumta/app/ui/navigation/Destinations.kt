package com.teumta.app.ui.navigation

import com.teumta.app.ui.input.RouteInput
import kotlinx.serialization.Serializable

// 화면 주소. Navigation이 이 클래스를 직렬화해서 다음 화면에 값을 넘긴다
@Serializable
object InputDestination

@Serializable
object InfoDestination

@Serializable
data class ResultDestination(
    val fromId: String,
    val viaId: String,
    val toId: String,
    // LocalTime은 그대로 넘길 수 없어서 0시부터의 분으로 넘긴다 (21:00 -> 1260)
    val arriveByMinute: Int,
    val stayMin: Int,
)

fun RouteInput.toDestination() = ResultDestination(
    fromId = fromId,
    viaId = viaId,
    toId = toId,
    arriveByMinute = arriveBy.toSecondOfDay() / 60,
    stayMin = stayMin,
)
