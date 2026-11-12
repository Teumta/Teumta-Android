package com.teumta.app.ui.info

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teumta.app.BuildConfig
import com.teumta.app.ui.theme.TeumtaTheme

@Composable
fun InfoScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    // 링크를 열 앱(브라우저·메일)이 없으면 openUri가 예외를 던져서 앱이 꺼진다. 대신 주소를 알려 준다
    val open: (String) -> Unit = { uri ->
        runCatching { uriHandler.openUri(uri) }
            .onFailure { Toast.makeText(context, "열 수 있는 앱이 없어요: $uri", Toast.LENGTH_LONG).show() }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("앱 정보", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onBack) { Text("닫기") }
            }

            Section("지원 구간") {
                Text("1호선 서울역~인천 (29개 역), 일반열차 기준이에요.")
                Text("급행은 빼고 계산해서 실제보다 조금 이른 열차가 나올 수 있어요. 실시간 지연은 반영하지 않아요.")
                Text("시간표 기준일: ${AppInfo.TIMETABLE_BASE_DATE}")
            }

            Section("데이터 출처") {
                Text(AppInfo.DATA_SOURCE_NAME)
                LinkButton("공공데이터포털에서 보기") { open(AppInfo.DATA_SOURCE_URL) }
                Text("틈타는 정부·철도 운영기관과 관계없는 개인 프로젝트예요.", style = MaterialTheme.typography.bodyMedium)
            }

            Section("문의와 개인정보") {
                LinkButton("문의 메일 보내기 (${AppInfo.CONTACT_EMAIL})") { open("mailto:${AppInfo.CONTACT_EMAIL}") }
                LinkButton("개인정보처리방침") { open(AppInfo.PRIVACY_URL) }
                Text("틈타는 개인정보를 모으지 않아요. 계산은 모두 기기 안에서 해요.", style = MaterialTheme.typography.bodyMedium)
            }

            Section("오픈소스 라이선스") {
                OPEN_SOURCE_LICENSES.forEachIndexed { index, oss ->
                    if (index > 0) HorizontalDivider()
                    Text(oss.name, style = MaterialTheme.typography.bodyLarge)
                    Text("${oss.owner} · ${oss.license}", style = MaterialTheme.typography.bodySmall)
                }
                LinkButton("Apache License 2.0 전문") { open(APACHE_LICENSE_URL) }
            }

            Text(
                "틈타 ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun LinkButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(text) }
}

@Preview(showBackground = true)
@Composable
private fun InfoScreenPreview() {
    TeumtaTheme { InfoScreen(onBack = {}) }
}
