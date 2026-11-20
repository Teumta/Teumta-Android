package com.teumta.app.data.account

import com.teumta.app.data.TimetableParser
import retrofit2.HttpException
import java.io.IOException

// 로그인이 풀렸을 때(refresh token까지 만료·폐기). 기기의 토큰은 이미 지웠다
class SessionExpiredException : Exception("로그인이 풀렸어요. 다시 로그인해 주세요.")

// 실패를 화면에 보여 줄 한국어 문장으로 바꾼다.
// 서버는 오류마다 한국어 message를 보내므로(ApiError) 그 문장을 그대로 쓰고, 입력 칸 오류가 있으면 그것을 먼저 쓴다
fun Throwable.toUserMessage(): String = when (this) {
    is SessionExpiredException -> message!!
    is HttpException -> apiError()?.let { it.fieldErrors.firstOrNull()?.message ?: it.message } ?: DEFAULT_MESSAGE
    is IOException -> "서버에 연결하지 못했어요. 인터넷 연결을 확인해 주세요."
    else -> DEFAULT_MESSAGE
}

private const val DEFAULT_MESSAGE = "잠시 후 다시 시도해 주세요."

// 오류 응답 본문을 ApiError로 읽는다. 본문이 없거나 모양이 다르면(서버 앞단의 오류 페이지 등) null
private fun HttpException.apiError(): ApiError? = runCatching {
    val body = response()?.errorBody()?.string() ?: return null
    TimetableParser.json.decodeFromString<ApiError>(body)
}.getOrNull()
