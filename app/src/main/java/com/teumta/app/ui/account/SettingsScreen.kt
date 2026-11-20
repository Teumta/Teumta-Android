package com.teumta.app.ui.account

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teumta.app.BuildConfig
import com.teumta.app.ui.info.AppInfo
import com.teumta.app.ui.input.SectionGap
import com.teumta.core.designsystem.component.TeumtaAppBar
import com.teumta.core.designsystem.component.TeumtaButton
import com.teumta.core.designsystem.component.TeumtaButtonStyle
import com.teumta.core.designsystem.component.TeumtaHelpButton
import com.teumta.core.designsystem.component.TeumtaListLabel
import com.teumta.core.designsystem.component.TeumtaListRow
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaShapes
import com.teumta.core.designsystem.theme.TeumtaTheme

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit,
    onInfoClick: () -> Unit,
    onFavoriteStationsClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBack = onBack,
        onLoginClick = onLoginClick,
        onSignupClick = onSignupClick,
        onLogout = viewModel::onLogout,
        onWithdraw = viewModel::onWithdraw,
        onInfoClick = onInfoClick,
        onFavoriteStationsClick = onFavoriteStationsClick,
    )
}

// 원래 기획 S-05: 위에 계정 한 줄, 회색 띠 아래 "자주 쓰는 두 역"과 "앱 정보" 목록, 맨 아래 회원 탈퇴
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit,
    onLogout: () -> Unit,
    onWithdraw: () -> Unit,
    onInfoClick: () -> Unit,
    onFavoriteStationsClick: () -> Unit,
) {
    // 탈퇴는 되돌릴 수 없어서 한 번 더 묻는다. 대화상자를 열었는지는 화면만 알면 된다
    var confirmWithdraw by rememberSaveable { mutableStateOf(false) }
    val colors = TeumtaTheme.colors
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Scaffold(
        containerColor = colors.bgSurface,
        topBar = { TeumtaAppBar(title = "설정", onBack = onBack, modifier = Modifier.statusBarsPadding()) },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.isLoggedIn) AccountRow(state, onLogout) else LoggedOutBlock(onLoginClick, onSignupClick)
            state.message?.let {
                Text(
                    text = it,
                    modifier = Modifier
                        .padding(start = TeumtaDimens.Space20, end = TeumtaDimens.Space20, bottom = TeumtaDimens.Space12)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    style = TeumtaTheme.typography.bodyS,
                    color = colors.textSecondary,
                )
            }

            // 저장해 둔 두 역. 로그인해야 저장할 수 있어서 로그인했을 때만 보인다
            if (state.isLoggedIn) {
                SectionGap()
                TeumtaListLabel("자주 쓰는 두 역", help = { TeumtaHelpButton("자주 쓰는 두 역", favoriteHelp(state.originName, state.destinationName)) })
                TeumtaListRow(label = "출발역", value = state.originName ?: "정하지 않음", onClick = onFavoriteStationsClick)
                TeumtaListRow(label = "도착역", value = state.destinationName ?: "정하지 않음", onClick = onFavoriteStationsClick, divider = false)
            }

            SectionGap()
            TeumtaListLabel("앱 정보")
            TeumtaListRow(
                label = "개인정보처리방침",
                onClick = {
                    // 링크를 열 브라우저가 없으면 openUri가 예외를 던져서 앱이 꺼진다. 대신 주소를 알려 준다
                    runCatching { uriHandler.openUri(AppInfo.PRIVACY_URL) }
                        .onFailure { Toast.makeText(context, "열 수 있는 앱이 없어요: ${AppInfo.PRIVACY_URL}", Toast.LENGTH_LONG).show() }
                },
            )
            TeumtaListRow(label = "앱 정보와 오픈소스 라이선스", sub = "지원 구간, 데이터 출처, 문의", onClick = onInfoClick)
            TeumtaListRow(label = "버전", value = BuildConfig.VERSION_NAME, divider = false)

            if (state.isLoggedIn) {
                Text(
                    text = "회원 탈퇴",
                    modifier = Modifier
                        .padding(start = 4.dp, top = TeumtaDimens.Space12, bottom = TeumtaDimens.Space24)
                        .clickable(enabled = !state.isBusy, role = Role.Button) { confirmWithdraw = true }
                        .heightIn(min = TeumtaDimens.SizeTouchMin)
                        .padding(horizontal = TeumtaDimens.Space16, vertical = 14.dp),
                    style = TeumtaTheme.typography.label,
                    color = colors.textTertiary,
                )
            }
        }
    }

    if (confirmWithdraw) {
        AlertDialog(
            onDismissRequest = { confirmWithdraw = false },
            title = { Text("탈퇴할까요?") },
            text = { Text("계정과 저장한 두 역이 서버에서 바로 지워지고 되돌릴 수 없어요.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmWithdraw = false
                    onWithdraw()
                }) { Text("탈퇴", color = colors.textDanger) }
            },
            dismissButton = { TextButton(onClick = { confirmWithdraw = false }) { Text("취소") } },
        )
    }
}

// 로그인했을 때: 이메일과 로그인 방법, 오른쪽에 작은 로그아웃 버튼
@Composable
private fun AccountRow(state: SettingsUiState, onLogout: () -> Unit) {
    val colors = TeumtaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = TeumtaDimens.Space20, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TeumtaDimens.Space12),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(state.email ?: "불러오는 중…", style = TeumtaTheme.typography.bodyStrong, color = colors.textPrimary)
            Text("이메일로 로그인", style = TeumtaTheme.typography.caption, color = colors.textTertiary)
        }
        Surface(
            onClick = onLogout,
            enabled = !state.isBusy,
            shape = TeumtaShapes.Sm,
            color = colors.bgSurface,
            border = BorderStroke(TeumtaDimens.StrokeHairline, colors.borderDefault),
        ) {
            Text(
                text = "로그아웃",
                modifier = Modifier.heightIn(min = 32.dp).padding(horizontal = TeumtaDimens.Space12, vertical = 6.dp),
                style = TeumtaTheme.typography.label,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
        }
    }
}

// 로그인하지 않았을 때: "계정" 이름(물음표에 로그인하면 되는 일)과 로그인·회원가입 버튼
@Composable
private fun LoggedOutBlock(onLoginClick: () -> Unit, onSignupClick: () -> Unit) {
    TeumtaListLabel("계정", help = { TeumtaHelpButton("로그인하면", ACCOUNT_HELP) })
    Row(
        modifier = Modifier.padding(start = TeumtaDimens.Space20, end = TeumtaDimens.Space20, top = TeumtaDimens.Space4, bottom = TeumtaDimens.Space20),
        horizontalArrangement = Arrangement.spacedBy(TeumtaDimens.Space8),
    ) {
        TeumtaButton(text = "로그인", onClick = onLoginClick, modifier = Modifier.weight(1f))
        TeumtaButton(text = "회원가입", onClick = onSignupClick, style = TeumtaButtonStyle.Outlined, modifier = Modifier.weight(1f))
    }
}

// 로그인·회원가입 화면도 같은 안내를 쓴다
internal val ACCOUNT_HELP = listOf(
    "로그인하면 자주 쓰는 두 역을 틈타 서버에 저장해요.",
    "다른 휴대폰에서 로그인해도 그대로 쓸 수 있어요.",
    "로그인하지 않아도 열차 계산은 모두 쓸 수 있어요.",
)

// 두 역을 정하면 입력 화면에 무엇이 생기는지. 정한 역이 있으면 그 이름으로 보여 준다
internal fun favoriteHelp(origin: String?, destination: String?): List<String> = listOfNotNull(
    if (origin != null && destination != null) {
        "입력 화면에 \"$origin → $destination\", \"$destination → $origin\" 버튼이 생겨요."
    } else {
        "출발역과 도착역을 모두 정하면 입력 화면에 두 역을 잇는 버튼이 생겨요."
    },
    "버튼을 누르면 출발과 도착이 바로 채워져요.",
    "틈타 서버에 저장해서 다른 휴대폰에서도 로그인하면 그대로 쓸 수 있어요.",
)

@Preview(showBackground = true)
@Composable
private fun SettingsLoggedOutPreview() {
    TeumtaTheme {
        SettingsScreen(SettingsUiState(), onBack = {}, onLoginClick = {}, onSignupClick = {}, onLogout = {}, onWithdraw = {}, onInfoClick = {}, onFavoriteStationsClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsLoggedInPreview() {
    TeumtaTheme {
        SettingsScreen(
            SettingsUiState(isLoggedIn = true, email = "me@example.com", originName = "동암", destinationName = "가산디지털단지"),
            onBack = {}, onLoginClick = {}, onSignupClick = {}, onLogout = {}, onWithdraw = {}, onInfoClick = {}, onFavoriteStationsClick = {},
        )
    }
}
