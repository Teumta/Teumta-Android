package com.teumta.app.ui.info

// 정보 화면에 보여 줄 값. 스토어 등록정보와 같은 값이어야 한다
object AppInfo {
    const val CONTACT_EMAIL = "teumta.app@gmail.com"
    const val PRIVACY_URL = "https://teumta.github.io/Teumta-Server/privacy.html"
    // 앱이 따르는 개인정보처리방침 판과 시행일. 방침을 개정하면 함께 올린다(올리면 바뀐 방침 안내가 한 번 다시 뜬다)
    const val PRIVACY_VERSION = "0.3.0"
    const val PRIVACY_EFFECTIVE_DATE = "2026년 11월 27일"
    const val DATA_SOURCE_NAME = "국토교통부 (TAGO) 지하철정보"
    const val DATA_SOURCE_URL = "https://www.data.go.kr/data/15098554/openapi.do"
    const val TIMETABLE_BASE_DATE = "2026년 11월 6일"
}

// 앱에 들어간 오픈소스. build.gradle.kts의 dependencies가 바뀌면 같이 고친다
data class OpenSourceLicense(val name: String, val owner: String, val license: String)

val OPEN_SOURCE_LICENSES = listOf(
    OpenSourceLicense("AndroidX (Core, Activity, Compose, Material 3, Lifecycle, Navigation, DataStore, Hilt)", "The Android Open Source Project", "Apache License 2.0"),
    OpenSourceLicense("Kotlin, kotlinx.coroutines, kotlinx.serialization", "JetBrains s.r.o.", "Apache License 2.0"),
    OpenSourceLicense("Dagger, Hilt", "The Dagger Authors", "Apache License 2.0"),
    OpenSourceLicense("Retrofit, OkHttp", "Square, Inc.", "Apache License 2.0"),
    OpenSourceLicense("Pretendard (글꼴)", "Kil Hyung-jin", "SIL Open Font License 1.1"),
)

const val APACHE_LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0"
const val OFL_LICENSE_URL = "https://openfontlicense.org/open-font-license-official-text/"
