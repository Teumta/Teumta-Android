package com.teumta.core.designsystem.component

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.teumta.core.designsystem.theme.TeumtaTheme
import org.junit.Rule
import org.junit.Test

private val SampleState = TodaySummaryState(
    stationName = "구로",
    tags = listOf("급행", "환승"),
    startMin = 18 * 60 + 30,
    goalMin = 21 * 60 + 30,
    arriveMin = 18 * 60 + 34,
    lastTrainMin = 21 * 60 + 6,
    lastTrainIsExpress = true,
)

class TodaySummaryTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readsStationAndSlack() {
        composeRule.setContent {
            TeumtaTheme { TodaySummary(state = SampleState, onClick = {}) }
        }
        composeRule
            .onNodeWithContentDescription("구로, 여유 2시간 32분", substring = true)
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}
