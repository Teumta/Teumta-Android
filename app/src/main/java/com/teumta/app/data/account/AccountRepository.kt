package com.teumta.app.data.account

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// 회원가입·로그인·로그아웃·탈퇴와 토큰 갱신을 한곳에서 한다.
// 화면(ViewModel)은 토큰을 몰라도 되고, 실패하면 예외를 받아 toUserMessage()로 보여 준다
@Singleton
class AccountRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: TokenStore,
) {
    private val refreshMutex = Mutex()

    // 토큰이 저장돼 있으면 로그인 상태. 로그아웃·탈퇴·갱신 실패로 지우면 바로 false가 된다
    val isLoggedIn: Flow<Boolean> = tokenStore.tokens.map { it != null }.distinctUntilChanged()

    // 서버에서 마지막으로 받은 자주 쓰는 두 역. 기기에는 저장하지 않고(개인정보처리방침: 서버에 저장) 앱이 켜져 있는 동안만 들고 있다.
    // 내 정보를 받거나 저장하면 바뀌고, 로그아웃·탈퇴하면 null이 된다
    private val _favoriteStations = MutableStateFlow<FavoriteStations?>(null)
    val favoriteStations: StateFlow<FavoriteStations?> = _favoriteStations.asStateFlow()

    // 가입하면 서버가 바로 토큰을 준다(가입 = 로그인)
    suspend fun signup(email: String, password: String) {
        save(api.signup(SignupRequest(email.trim(), password)))
    }

    suspend fun login(email: String, password: String) {
        save(api.login(LoginRequest(email.trim(), password)))
    }

    // 서버에서 지우지 못해도(인터넷 없음 등) 기기에서는 로그아웃한다.
    // 서버에 남은 refresh token은 14일 뒤 쓸 수 없고, 다음 로그인 때 지워진다
    suspend fun logout() {
        val tokens = tokenStore.tokens.first() ?: return
        try {
            api.logout(RefreshRequest(tokens.refreshToken))
        } catch (e: IOException) {
            // 서버에 못 붙음. 기기의 토큰만 지운다
        } catch (e: HttpException) {
            // 서버 오류. 기기의 토큰만 지운다
        }
        clearLocal()
    }

    suspend fun getMe(): MemberResponse = authorized { api.getMe(it) }.also(::remember)

    // 자주 쓰는 두 역을 서버에 저장한다. 같은 역이나 없는 역이면 서버가 400과 이유를 보낸다
    suspend fun updateStations(originStationId: String?, destinationStationId: String?): MemberResponse =
        authorized { api.updateStations(it, UpdateStationsRequest(originStationId, destinationStationId)) }.also(::remember)

    // 서버에서 지운 것이 확인된 뒤에만 기기의 토큰을 지운다. 실패하면 예외가 나가고 로그인 상태는 그대로다
    suspend fun withdraw() {
        authorized { api.withdraw(it) }
        clearLocal()
    }

    private fun remember(me: MemberResponse) {
        _favoriteStations.value = FavoriteStations(me.originStationId, me.destinationStationId)
    }

    // 기기에서 로그아웃한다. 토큰과 들고 있던 두 역을 함께 지운다
    private suspend fun clearLocal() {
        tokenStore.clear()
        _favoriteStations.value = null
    }

    private suspend fun save(response: TokenResponse) {
        tokenStore.save(Tokens(response.accessToken, response.refreshToken))
    }

    // 로그인이 필요한 요청을 보낸다. access token(30분)이 만료돼 401이 오면
    // refresh token으로 한 번 갱신하고 다시 보낸다. 그래도 안 되면 로그아웃한다
    private suspend fun <T> authorized(call: suspend (authorization: String) -> T): T {
        val used = tokenStore.tokens.first() ?: throw SessionExpiredException()
        try {
            return call(bearer(used.accessToken))
        } catch (e: HttpException) {
            if (e.code() != 401) throw e
        }
        val fresh = refresh(used)
        try {
            return call(bearer(fresh.accessToken))
        } catch (e: HttpException) {
            // 새 토큰으로도 401이면 회원이 없다(다른 기기에서 탈퇴 등)
            if (e.code() == 401) logoutLocally()
            throw e
        }
    }

    // 두 요청이 동시에 401을 받아도 갱신은 한 번만 한다.
    // refresh token은 한 번 쓰면 서버가 버려서, 같은 값으로 두 번 갱신하면 두 번째가 실패해 로그아웃되기 때문이다
    private suspend fun refresh(used: Tokens): Tokens = refreshMutex.withLock {
        val current = tokenStore.tokens.first() ?: throw SessionExpiredException()
        // 기다리는 동안 다른 요청이 이미 갱신했다면 그 토큰을 쓴다
        if (current.accessToken != used.accessToken) return@withLock current
        try {
            val response = api.refresh(RefreshRequest(current.refreshToken))
            Tokens(response.accessToken, response.refreshToken).also { tokenStore.save(it) }
        } catch (e: HttpException) {
            // refresh token도 만료(14일)됐거나 다른 곳에서 로그아웃·탈퇴했다
            if (e.code() == 401) logoutLocally()
            throw e
        }
    }

    private suspend fun logoutLocally(): Nothing {
        clearLocal()
        throw SessionExpiredException()
    }

    private fun bearer(accessToken: String) = "Bearer $accessToken"
}

// 회원이 저장한 자주 쓰는 두 역 ID. 고르지 않았으면 null
data class FavoriteStations(val originStationId: String?, val destinationStationId: String?)
