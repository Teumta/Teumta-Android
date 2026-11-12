package com.teumta.app.ui.result

import com.teumta.app.domain.CalculationDay
import com.teumta.app.domain.DepartureResult

sealed interface ResultUiState {
    data object Loading : ResultUiState

    data class Ready(
        val day: CalculationDay,
        val result: DepartureResult,
        val stationNames: Map<String, String>,
        val stayMin: Int,
    ) : ResultUiState {
        fun nameOf(stationId: String): String = stationNames[stationId] ?: stationId
    }
}
