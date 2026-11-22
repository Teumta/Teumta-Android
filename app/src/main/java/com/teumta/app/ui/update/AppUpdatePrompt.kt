package com.teumta.app.ui.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.teumta.app.R
import com.teumta.core.designsystem.component.TeumtaButton
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaTheme

// Play 스토어에 새 버전이 올라와 있으면 앱을 켤 때 한 번 알려 준다(Google Play 인앱 업데이트).
// "업데이트"를 누르면 앱 안에서 Play 스토어 업데이트 화면이 열린다. 그게 안 되면 Play 스토어의 틈타 페이지를 연다.
// Play 스토어에서 설치한 앱에서만 동작한다(개발 중 직접 설치한 앱은 새 버전이 없다고 나와서 안 뜬다)
@Composable
fun AppUpdatePrompt() {
    val context = LocalContext.current
    val manager = remember { AppUpdateManagerFactory.create(context) }
    var update by remember { mutableStateOf<AppUpdateInfo?>(null) }
    // "나중에"를 누르면 이번에 켠 동안은 다시 묻지 않는다(화면을 돌려도 유지)
    var later by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}

    // Play 스토어에 묻는다. Play 스토어가 없거나 Play에서 설치하지 않은 앱이면 실패하는데, 그때는 조용히 넘어간다
    LaunchedEffect(manager) {
        update = runCatching { manager.requestAppUpdateInfo() }.getOrNull()
            ?.takeIf { shouldOfferUpdate(it.updateAvailability()) }
    }

    // 바로 업데이트 도중 앱을 나갔다 오면, Play가 이어서 업데이트하도록 다시 연다
    LifecycleResumeEffect(manager) {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
            }
        }
        onPauseOrDispose {}
    }

    val info = update
    if (info != null && !later) {
        UpdateDialog(
            onUpdate = {
                later = true
                val started = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) &&
                    manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
                if (!started) openPlayStore(context)
            },
            onLater = { later = true },
        )
    }
}

// 새 버전 안내. 가운데에 앱 아이콘, 짧은 제목과 한 문장, 아래에 큰 "업데이트"와 "나중에"
@Composable
fun UpdateDialog(onUpdate: () -> Unit, onLater: () -> Unit) {
    val colors = TeumtaTheme.colors
    Dialog(onDismissRequest = onLater) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgSurface, RoundedCornerShape(24.dp))
                .padding(start = TeumtaDimens.Space24, end = TeumtaDimens.Space24, top = 28.dp, bottom = TeumtaDimens.Space16),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(Modifier.size(72.dp))
            Text(
                text = "새 버전이 나왔어요",
                modifier = Modifier.padding(top = 20.dp).semantics { heading() },
                style = TeumtaTheme.typography.headline,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "새 기능을 바로 써 보세요.",
                modifier = Modifier.padding(top = TeumtaDimens.Space8),
                style = TeumtaTheme.typography.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Play 스토어에서 업데이트해요",
                modifier = Modifier.padding(top = TeumtaDimens.Space4),
                style = TeumtaTheme.typography.caption,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )
            TeumtaButton(
                text = "업데이트",
                onClick = onUpdate,
                modifier = Modifier.fillMaxWidth().padding(top = TeumtaDimens.Space24),
            )
            TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth().padding(top = TeumtaDimens.Space4)) {
                Text("나중에", style = TeumtaTheme.typography.body, color = colors.textSecondary)
            }
        }
    }
}

// 홈 화면의 앱 아이콘과 같은 모양: 주황 바탕 위에 핀 그림(ic_launcher_foreground).
// 적응형 아이콘은 108 격자 중 가운데 72만 보이므로, 그림을 상자의 1.5배로 그려 가장자리를 잘라 낸다
@Composable
private fun AppIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22))
            .background(colorResource(R.color.teumta_orange)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "틈타 앱 아이콘",
            modifier = Modifier.fillMaxSize().wrapContentSize(unbounded = true).requiredSize(108.dp),
        )
    }
}

// Play 스토어에 이 앱보다 새 버전이 있을 때만 묻는다
internal fun shouldOfferUpdate(availability: Int): Boolean = availability == UpdateAvailability.UPDATE_AVAILABLE

// Play 스토어 앱으로 틈타 페이지를 연다. 없으면 브라우저로, 그것도 없으면 주소를 알려 준다
internal fun playStoreUris(packageName: String): List<String> = listOf(
    "market://details?id=$packageName",
    "https://play.google.com/store/apps/details?id=$packageName",
)

private fun openPlayStore(context: Context) {
    val uris = playStoreUris(context.packageName)
    for (uri in uris) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (e: ActivityNotFoundException) {
            // 다음 주소로
        }
    }
    Toast.makeText(context, "열 수 있는 앱이 없어요: ${uris.last()}", Toast.LENGTH_LONG).show()
}
