import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// 업로드 키 정보는 저장소에 올리지 않는 keystore.properties(.gitignore)에서 읽는다.
// 파일이 없으면 release 빌드는 서명하지 않은 채로 만들어진다.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.teumta.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.teumta.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 시간표 서버 주소. 기본값은 개발용으로, 에뮬레이터(기기)의 localhost를 내 컴퓨터의 서버로 잇는다.
        // 쓰기 전에 한 번: adb reverse tcp:8080 tcp:8080
        // (Android 17부터 앱이 10.0.2.2 같은 로컬 네트워크 주소에 붙으려면 권한이 필요해서 localhost로 잇는다)
        // 배포한 서버를 쓰려면 gradle.properties나 명령줄(-Pteumta.apiBaseUrl=https://...)로 바꾼다
        val apiBaseUrl = providers.gradleProperty("teumta.apiBaseUrl").getOrElse("http://localhost:8080/")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    signingConfigs {
        create("release") {
            if (!keystoreProperties.isEmpty) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (!keystoreProperties.isEmpty) signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = true
                // 일부 패키지만 줄이면 R8이 Kotlin 표준 라이브러리 클래스를 다른 패키지로 옮겨 시작하자마자 꺼졌다(v0.2.0 준비 중).
                // 기본값인 전체(**)로 줄인다. 라이브러리들은 필요한 keep 규칙을 함께 배포한다
                packageScope = setOf("**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
