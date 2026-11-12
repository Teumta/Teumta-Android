package com.teumta.app.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teumta.app.domain.DeparturePlan
import com.teumta.app.domain.DepartureResult
import com.teumta.app.domain.FailReason
import com.teumta.app.ui.formatClock
import com.teumta.app.ui.formatDuration
import com.teumta.app.ui.formatServiceDay

@Composable
fun ResultRoute(
    onBack: () -> Unit,
    viewModel: ResultViewModel = viewModel(factory = ResultViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ResultScreen(state = state, onBack = onBack, onNextDay = viewModel::showNextDay)
}

@Composable
fun ResultScreen(state: ResultUiState, onBack: () -> Unit, onNextDay: () -> Unit) {
    Scaffold { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (state) {
                ResultUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is ResultUiState.Ready -> Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(formatServiceDay(state.day.date, state.day.dayType), style = MaterialTheme.typography.labelLarge)
                    when (val result = state.result) {
                        is DepartureResult.Success -> SuccessContent(state, result)
                        is DepartureResult.Failure -> FailureContent(result.reason, onNextDay)
                    }
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("다시 입력") }
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(state: ResultUiState.Ready, result: DepartureResult.Success) {
    val plan = result.plan
    val from = state.nameOf(plan.first.fromId)

    Column {
        Text("늦어도", style = MaterialTheme.typography.titleMedium)
        Text(
            text = formatClock(plan.departSec),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text("${from}에서 ${plan.first.train.headsign}행 열차를 타세요", style = MaterialTheme.typography.bodyLarge)
        state.day.nowSec?.let { now ->
            Text("지금부터 ${formatDuration(plan.departSec - now)} 뒤", style = MaterialTheme.typography.bodyMedium)
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("동선", style = MaterialTheme.typography.titleMedium)
            Timeline(state, plan)
        }
    }

    result.alternative?.let { alt ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("한 대 앞 열차", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${formatClock(alt.departSec)} 출발 · ${state.nameOf(alt.first.toId)}에서 ${formatDuration(alt.staySec)} 머무를 수 있어요",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// 출발역 승차 → 경유역 하차 → 머무름 → 경유역 승차 → 도착역 하차
@Composable
private fun Timeline(state: ResultUiState.Ready, plan: DeparturePlan) {
    val via = state.nameOf(plan.first.toId)
    TimelineRow(formatClock(plan.first.departSec), "${state.nameOf(plan.first.fromId)} 승차", "${plan.first.train.headsign}행")
    TimelineRow(formatClock(plan.first.arriveSec), "$via 하차", null)
    TimelineRow(
        time = "",
        title = "${via}에서 ${formatDuration(plan.staySec)} 머무름",
        detail = "원한 시간 ${formatDuration(state.stayMin * 60)} · 내리고 타는 여유 2분은 뺐어요",
        highlight = true,
    )
    TimelineRow(formatClock(plan.second.departSec), "$via 승차", "${plan.second.train.headsign}행")
    TimelineRow(formatClock(plan.second.arriveSec), "${state.nameOf(plan.second.toId)} 도착", null)
}

@Composable
private fun TimelineRow(time: String, title: String, detail: String?, highlight: Boolean = false) {
    Row {
        Text(time, modifier = Modifier.width(56.dp), style = MaterialTheme.typography.titleMedium)
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (highlight) FontWeight.Bold else null,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FailureContent(reason: FailReason, onNextDay: () -> Unit) {
    val (title, body) = when (reason) {
        FailReason.NO_TRAIN_BEFORE_ARRIVAL ->
            "그 시각까지 도착하는 열차가 없어요" to "첫차보다 이른 시각이에요. 도착 시각을 늦춰 주세요."
        FailReason.STAY_TOO_LONG ->
            "그만큼 머무르면 시간이 모자라요" to "머무를 시간을 줄이거나 도착 시각을 늦춰 주세요."
        FailReason.ALREADY_DEPARTED ->
            "탈 수 있는 열차가 이미 떠났어요" to "도착 시각을 늦추거나, 다음 운행일 같은 시각으로 볼 수 있어요."
    }
    Spacer(Modifier.height(24.dp))
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(body, style = MaterialTheme.typography.bodyLarge)
    if (reason == FailReason.ALREADY_DEPARTED) {
        Button(onClick = onNextDay, modifier = Modifier.fillMaxWidth()) { Text("다음 운행일로 보기") }
    }
}
