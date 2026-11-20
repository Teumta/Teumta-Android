package com.teumta.app.ui.info

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.teumta.app.BuildConfig
import com.teumta.app.ui.input.SectionGap
import com.teumta.core.designsystem.component.TeumtaAppBar
import com.teumta.core.designsystem.component.TeumtaHelpButton
import com.teumta.core.designsystem.component.TeumtaListLabel
import com.teumta.core.designsystem.component.TeumtaListRow
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaTheme

// 앱 정보: 설정 화면과 같은 목록 모양. 지원 구간, 데이터 출처, 문의와 개인정보, 오픈소스 라이선스
@Composable
fun InfoScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    // 링크를 열 앱(브라우저·메일)이 없으면 openUri가 예외를 던져서 앱이 꺼진다. 대신 주소를 알려 준다
    val open: (String) -> Unit = { uri ->
        runCatching { uriHandler.openUri(uri) }
            .onFailure { Toast.makeText(context, "열 수 있는 앱이 없어요: $uri", Toast.LENGTH_LONG).show() }
    }

    Scaffold(
        containerColor = TeumtaTheme.colors.bgSurface,
        topBar = { TeumtaAppBar(title = "앱 정보", onBack = onBack, modifier = Modifier.statusBarsPadding()) },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // 설명은 줄글 대신 이름·값 줄로, 더 긴 설명은 물음표 안내로 둔다
            TeumtaListLabel("지원 구간", help = { TeumtaHelpButton("지원 구간", COVERAGE_HELP) })
            TeumtaListRow(label = "노선", value = "1호선 102개 역")
            TeumtaListRow(label = "구간", value = "연천~인천·신창, 광명, 서동탄")
            TeumtaListRow(label = "시간표 기준일", value = AppInfo.TIMETABLE_BASE_DATE)
            TeumtaListRow(label = "실시간 지연", value = "반영하지 않음", divider = false)

            SectionGap()
            TeumtaListLabel("데이터 출처", help = { TeumtaHelpButton("데이터 출처", SOURCE_HELP) })
            TeumtaListRow(label = AppInfo.DATA_SOURCE_NAME, sub = "공공데이터포털에서 보기", onClick = { open(AppInfo.DATA_SOURCE_URL) }, divider = false)

            SectionGap()
            TeumtaListLabel("문의와 개인정보", help = { TeumtaHelpButton("개인정보", PRIVACY_HELP) })
            TeumtaListRow(label = "문의 메일", sub = AppInfo.CONTACT_EMAIL, onClick = { open("mailto:${AppInfo.CONTACT_EMAIL}") })
            TeumtaListRow(label = "개인정보처리방침", onClick = { open(AppInfo.PRIVACY_URL) }, divider = false)

            SectionGap()
            TeumtaListLabel("오픈소스 라이선스")
            OPEN_SOURCE_LICENSES.forEach { oss ->
                TeumtaListRow(label = oss.name, sub = "${oss.owner} · ${oss.license}")
            }
            TeumtaListRow(label = "Apache License 2.0 전문", onClick = { open(APACHE_LICENSE_URL) })
            TeumtaListRow(label = "SIL Open Font License 1.1 전문", onClick = { open(OFL_LICENSE_URL) }, divider = false)

            Text(
                text = "틈타 ${BuildConfig.VERSION_NAME}",
                modifier = Modifier.fillMaxWidth().padding(TeumtaDimens.Space24),
                style = TeumtaTheme.typography.caption,
                color = TeumtaTheme.colors.textTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val COVERAGE_HELP = listOf(
    "급행과 갈아타기까지 계산해요.",
    "갈아타는 시간은 역마다 2~5분으로 잡아요.",
    "실시간 지연은 반영하지 않아서, 열차가 늦으면 실제와 다를 수 있어요.",
)

private val SOURCE_HELP = listOf(
    "시간표는 공공데이터포털의 국토교통부(TAGO) 지하철정보를 받아 만들었어요.",
    "틈타는 정부·철도 운영기관과 관계없는 개인 프로젝트예요.",
)

private val PRIVACY_HELP = listOf(
    "열차 계산은 휴대폰 안에서 해요.",
    "회원가입하면 이메일과 자주 쓰는 두 역을 서버에 저장해요.",
    "탈퇴하면 서버에서 바로 지워요.",
)

@Preview(showBackground = true)
@Composable
private fun InfoScreenPreview() {
    TeumtaTheme { InfoScreen(onBack = {}) }
}
