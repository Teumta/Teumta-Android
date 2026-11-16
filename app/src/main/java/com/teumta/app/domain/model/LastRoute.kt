package com.teumta.app.domain.model

// 마지막으로 계산한 경로. 도착 시각은 그때그때 달라서 저장하지 않는다
data class LastRoute(
    val fromId: String,
    val viaId: String,
    val toId: String,
    val stayMin: Int,
)
