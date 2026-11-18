package com.teumta.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.teumta.core.designsystem.R

// Pretendard (SIL OFL 1.1). res/font에 굵기 5개 파일을 넣어요.
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
)

// Figma와 같은 줄 높이: 글꼴 여백 없이 행간 가운데 정렬
private val Base = TextStyle(
    fontFamily = Pretendard,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

// 텍스트 스타일 15개 (Figma: 틈타/…)
@Immutable
data class TeumtaTypography(
    // 나와야 하는 시각
    val display: TextStyle = Base.copy(
        fontSize = 56.sp,
        lineHeight = 62.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.03).em,
        fontFeatureSettings = "tnum",
    ),
    // 홈 여유 시간
    val headlineL: TextStyle = Base.copy(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.02).em,
        fontFeatureSettings = "tnum",
    ),
    // 화면 제목 (온보딩, 로그인)
    val headlineM: TextStyle = Base.copy(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.02).em,
    ),
    // 상태 제목
    val headline: TextStyle = Base.copy(
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.02).em,
    ),
    // 홈 가장 여유 있는 역
    val titleL: TextStyle = Base.copy(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.01).em,
    ),
    // 목록 오른쪽 시간
    val numberL: TextStyle = Base.copy(
        fontSize = 17.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.01).em,
        fontFeatureSettings = "tnum",
    ),
    // 앱바 제목
    val title: TextStyle = Base.copy(
        fontSize = 17.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.01).em,
    ),
    // 답 아래 수치
    val stat: TextStyle = Base.copy(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.01).em,
        fontFeatureSettings = "tnum",
    ),
    // 버튼, 구역 제목
    val titleS: TextStyle = Base.copy(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    // 목록 이름, 설정 항목
    val bodyStrong: TextStyle = Base.copy(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    // 입력 값, 본문
    val body: TextStyle = Base.copy(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
    ),
    // 보조 문장
    val bodyS: TextStyle = Base.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
    // 필드 라벨, 구역 머리
    val label: TextStyle = Base.copy(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    ),
    // 메타, 오류, 근거
    val caption: TextStyle = Base.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
    ),
    // 지도 역 이름, 축 라벨
    val micro: TextStyle = Base.copy(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)

// Material 3 컴포넌트가 쓰는 글자 슬롯에 연결
fun TeumtaTypography.toMaterialTypography() = Typography(
    displayLarge = display,
    headlineLarge = headlineL,
    headlineMedium = headlineM,
    headlineSmall = headline,
    titleLarge = titleL,
    titleMedium = title,
    titleSmall = titleS,
    bodyLarge = body,
    bodyMedium = bodyS,
    bodySmall = caption,
    labelLarge = titleS,  // 버튼 글자
    labelMedium = label,
    labelSmall = micro,
)
