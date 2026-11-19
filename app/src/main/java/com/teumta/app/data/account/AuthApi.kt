package com.teumta.app.data.account

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

// 회원 API(Teumta-Server의 /api/auth, /api/members/me).
// 로그인이 필요한 API는 access token을 Authorization 헤더로 직접 받는다.
// (OkHttp가 모든 요청에 토큰을 붙이게 하면, 만료된 토큰이 붙은 시간표 요청까지 401로 막힌다)
interface AuthApi {

    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): TokenResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    // 쓴 refresh token은 서버가 버리고 새 토큰 한 쌍을 준다(회전)
    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): TokenResponse

    // 이 기기의 refresh token을 서버에서 지운다. 204(본문 없음)라서 Unit
    @POST("api/auth/logout")
    suspend fun logout(@Body request: RefreshRequest)

    @GET("api/members/me")
    suspend fun getMe(@Header("Authorization") authorization: String): MemberResponse

    @DELETE("api/members/me")
    suspend fun withdraw(@Header("Authorization") authorization: String)

    // 집역·회사역을 함께 바꾼다. null이면 그 역을 지운다. 바뀐 내 정보를 돌려준다
    @PUT("api/members/me/stations")
    suspend fun updateStations(
        @Header("Authorization") authorization: String,
        @Body request: UpdateStationsRequest,
    ): MemberResponse
}

@Serializable
data class SignupRequest(val email: String, val password: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

// null도 그대로 보낸다(JSON의 null). 서버는 null을 "지우기"로 읽는다
@Serializable
data class UpdateStationsRequest(val homeStationId: String?, val workStationId: String?)

// expiresIn(초)은 받지만 쓰지 않는다. 만료는 서버가 401로 알려 주면 그때 갱신한다
@Serializable
data class TokenResponse(val accessToken: String, val refreshToken: String)

// 고르지 않은 역은 null. createdAt 같은 다른 칸은 아직 쓰지 않아서 건너뛴다(ignoreUnknownKeys)
@Serializable
data class MemberResponse(
    val id: Long,
    val email: String,
    val homeStationId: String? = null,
    val workStationId: String? = null,
)

// 서버의 모든 오류 응답 모양 {code, message, fieldErrors}
@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val fieldErrors: List<FieldError> = emptyList(),
) {
    @Serializable
    data class FieldError(val field: String, val message: String)
}
