package com.teumta.app.ui.result

import com.teumta.app.domain.CalculationDay
import com.teumta.app.domain.DepartureResult
import com.teumta.app.domain.model.TimetableSource

sealed interface ResultUiState {
    data object Loading : ResultUiState

    data class Ready(
        val day: CalculationDay,
        val result: DepartureResult,
        val stationNames: Map<String, String>,
        val stayMin: Int,
        // 계산에 쓴 시간표가 어디서 왔고 언제 기준인지
        val timetableSource: TimetableSource,
        val baseDate: String,
    ) : ResultUiState {
        fun nameOf(stationId: String): String = stationNames[stationId] ?: stationId
    }
}
