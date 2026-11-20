package com.teumta.app.di

import com.teumta.app.BuildConfig
import com.teumta.app.data.TimetableApi
import com.teumta.app.data.TimetableParser
import com.teumta.app.data.account.AuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// 서버와 통신하는 객체들을 만든다. 만들 때 비용이 커서 앱 전체에서 하나씩만 쓴다(@Singleton)
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        // 서버가 꺼져 있으면 오래 기다리지 않고 앱 안 시간표로 넘어가게 짧게 둔다
        .connectTimeout(3, TimeUnit.SECONDS)
        // 시간표는 gzip으로 약 200KB라, 느린 망에서도 받을 수 있게 읽기는 넉넉히
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        // JSON은 앱 안 파일을 읽을 때와 같은 규칙(모르는 칸 건너뛰기)으로 읽는다
        .addConverterFactory(TimetableParser.json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideTimetableApi(retrofit: Retrofit): TimetableApi = retrofit.create(TimetableApi::class.java)

    // 회원 API도 같은 서버라서 같은 Retrofit으로 만든다
    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
}
