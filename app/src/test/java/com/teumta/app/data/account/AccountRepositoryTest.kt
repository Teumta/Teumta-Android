package com.teumta.app.data.account

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.teumta.app.data.TimetableParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

// 가짜 서버(MockWebServer)와 임시 폴더의 DataStore로, 실제 서버 없이 가입·로그인·토큰 갱신·로그아웃·탈퇴를 확인한다
class AccountRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val server = MockWebServer()
    private lateinit var api: AuthApi

    @Before
    fun setUp() {
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(TimetableParser.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun tokenStore(scope: CoroutineScope) = TokenStore(
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(folder.root, "test.preferences_pb") }),
    )

    @Test
    fun 가입하면_토큰을_저장하고_로그인_상태가_된다() = runTest {
        val store = tokenStore(backgroundScope)
        val repository = AccountRepository(api, store)
        server.enqueue(json(201, tokens("A1", "R1")))

        repository.signup(" me@example.com ", "password1")

        val request = server.takeRequest()
        assertEquals("/api/auth/signup", request.target)
        // 앞뒤 빈칸은 지워서 보낸다
        assertEquals("""{"email":"me@example.com","password":"password1"}""", request.body?.utf8())
        assertEquals(Tokens("A1", "R1"), store.tokens.first())
        assertTrue(repository.isLoggedIn.first())
    }

    @Test
    fun 로그인에_실패하면_서버의_문장을_보여_주고_로그인되지_않는다() = runTest {
        val repository = AccountRepository(api, tokenStore(backgroundScope))
        server.enqueue(json(401, """{"code":"LOGIN_FAILED","message":"이메일 또는 비밀번호가 맞지 않아요.","fieldErrors":[]}"""))

        val error = runCatching { repository.login("me@example.com", "wrong-pass") }.exceptionOrNull()

        assertEquals("이메일 또는 비밀번호가 맞지 않아요.", error?.toUserMessage())
        assertFalse(repository.isLoggedIn.first())
    }

    @Test
    fun 입력_오류는_칸별_문장을_먼저_보여_준다() = runTest {
        val repository = AccountRepository(api, tokenStore(backgroundScope))
        server.enqueue(
            json(400, """{"code":"INVALID_INPUT","message":"입력값을 확인해 주세요.","fieldErrors":[{"field":"email","message":"이메일 형식이 아니에요."}]}"""),
        )

        val error = runCatching { repository.signup("me", "password1") }.exceptionOrNull()

        assertEquals("이메일 형식이 아니에요.", error?.toUserMessage())
    }

    @Test
    fun 서버에_못_붙으면_인터넷_안내를_보여_준다() = runTest {
        val repository = AccountRepository(api, tokenStore(backgroundScope))
        server.close()

        val error = runCatching { repository.login("me@example.com", "password1") }.exceptionOrNull()

        assertEquals("서버에 연결하지 못했어요. 인터넷 연결을 확인해 주세요.", error?.toUserMessage())
    }

    @Test
    fun access_token이_만료되면_갱신하고_다시_보낸다() = runTest {
        val store = tokenStore(backgroundScope).apply { save(Tokens("old-access", "R1")) }
        val repository = AccountRepository(api, store)
        server.enqueue(json(401, """{"code":"UNAUTHORIZED","message":"로그인이 필요해요.","fieldErrors":[]}"""))
        server.enqueue(json(200, tokens("new-access", "R2")))
        server.enqueue(json(200, """{"id":1,"email":"me@example.com","createdAt":"2026-11-19T00:00:00Z","originStationId":null,"destinationStationId":null}"""))

        val me = repository.getMe()

        assertEquals("me@example.com", me.email)
        assertEquals("Bearer old-access", server.takeRequest().headers["Authorization"])
        val refresh = server.takeRequest()
        assertEquals("/api/auth/refresh", refresh.target)
        assertEquals("""{"refreshToken":"R1"}""", refresh.body?.utf8())
        assertEquals("Bearer new-access", server.takeRequest().headers["Authorization"])
        // 회전: 새 토큰 한 쌍으로 바뀌었다
        assertEquals(Tokens("new-access", "R2"), store.tokens.first())
    }

    @Test
    fun 갱신도_실패하면_로그아웃되고_다시_로그인하라고_한다() = runTest {
        val store = tokenStore(backgroundScope).apply { save(Tokens("old-access", "R1")) }
        val repository = AccountRepository(api, store)
        server.enqueue(json(401, """{"code":"UNAUTHORIZED","message":"로그인이 필요해요.","fieldErrors":[]}"""))
        server.enqueue(json(401, """{"code":"INVALID_REFRESH_TOKEN","message":"다시 로그인해 주세요.","fieldErrors":[]}"""))

        val error = runCatching { repository.getMe() }.exceptionOrNull()

        assertTrue(error is SessionExpiredException)
        assertFalse(repository.isLoggedIn.first())
    }

    @Test
    fun 두_요청이_함께_만료돼도_갱신은_한_번만_한다() = runTest {
        val store = tokenStore(backgroundScope).apply { save(Tokens("old-access", "R1")) }
        val repository = AccountRepository(api, store)
        val refreshCalls = AtomicInteger()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.target == "/api/auth/refresh" -> {
                    refreshCalls.incrementAndGet()
                    // 같은 refresh token을 두 번 쓰면 서버처럼 거절한다
                    if (request.body?.utf8()?.contains("R1") == true) json(200, tokens("new-access", "R2"))
                    else json(401, """{"code":"INVALID_REFRESH_TOKEN","message":"다시 로그인해 주세요.","fieldErrors":[]}""")
                }
                request.headers["Authorization"] == "Bearer new-access" ->
                    json(200, """{"id":1,"email":"me@example.com"}""")
                else -> json(401, """{"code":"UNAUTHORIZED","message":"로그인이 필요해요.","fieldErrors":[]}""")
            }
        }

        val results = listOf(async { repository.getMe() }, async { repository.getMe() }).awaitAll()

        assertEquals(listOf("me@example.com", "me@example.com"), results.map { it.email })
        assertEquals(1, refreshCalls.get())
        assertTrue(repository.isLoggedIn.first())
    }

    @Test
    fun 로그아웃은_서버에_못_붙어도_기기에서_로그아웃한다() = runTest {
        val store = tokenStore(backgroundScope).apply { save(Tokens("A1", "R1")) }
        val repository = AccountRepository(api, store)
        server.close()

        repository.logout()

        assertFalse(repository.isLoggedIn.first())
    }

    @Test
    fun 로그아웃하면_서버에_refresh_token을_보내고_지운다() = runTest {
        val store = tokenStore(backgroundScope).apply { save(Tokens("A1", "R1")) }
        val repository = AccountRepository(api, store)
        server.enqueue(MockResponse.Builder().code(204).build())

        repository.logout()

        val request = server.takeRequest()
        assertEquals("/api/auth/logout", request.target)
        assertEquals("""{"refreshToken":"R1"}""", request.body?.utf8())
        assertFalse(repository.isLoggedIn.first())
    }

    @Test
    fun 탈퇴하면_로그아웃된다() = runTest {
        val store = tokenStore(backgroundScope).apply { save(Tokens("A1", "R1")) }
        val repository = AccountRepository(api, store)
        server.enqueue(MockResponse.Builder().code(204).build())

        repository.withdraw()

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("Bearer A1", request.headers["Authorization"])
        assertFalse(repository.isLoggedIn.first())
    }

    @Test
    fun 탈퇴가_실패하면_로그인_상태를_그대로_둔다() = runTest {
        val store = tokenStore(backgroundScope).apply { save(Tokens("A1", "R1")) }
        val repository = AccountRepository(api, store)
        server.enqueue(json(500, """{"code":"INTERNAL_ERROR","message":"잠시 후 다시 시도해 주세요.","fieldErrors":[]}"""))

        try {
            repository.withdraw()
            fail("탈퇴가 실패해야 한다")
        } catch (e: Exception) {
            assertEquals("잠시 후 다시 시도해 주세요.", e.toUserMessage())
        }
        assertTrue(repository.isLoggedIn.first())
    }

    @Test
    fun 두_역을_저장하면_서버에_보내고_들고_있는다() = runTest {
        val store = tokenStore(backgroundScope).apply { save(Tokens("A1", "R1")) }
        val repository = AccountRepository(api, store)
        server.enqueue(json(200, """{"id":1,"email":"me@example.com","originStationId":"MTRKR1161","destinationStationId":null}"""))

        repository.updateStations("MTRKR1161", null)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/members/me/stations", request.target)
        assertEquals("Bearer A1", request.headers["Authorization"])
        // 지울 역은 빼지 않고 null로 보낸다
        assertEquals("""{"originStationId":"MTRKR1161","destinationStationId":null}""", request.body?.utf8())
        assertEquals(FavoriteStations("MTRKR1161", null), repository.favoriteStations.value)
    }

    @Test
    fun 같은_역이면_서버의_이유를_보여_주고_들고_있던_값은_그대로다() = runTest {
        val store = tokenStore(backgroundScope).apply { save(Tokens("A1", "R1")) }
        val repository = AccountRepository(api, store)
        server.enqueue(json(200, """{"id":1,"email":"me@example.com","originStationId":"MTRKR1161","destinationStationId":"MTRS11150"}"""))
        repository.getMe()
        server.enqueue(json(400, """{"code":"SAME_STATIONS","message":"출발역과 도착역을 다르게 골라 주세요.","fieldErrors":[]}"""))

        val error = runCatching { repository.updateStations("MTRKR1141", "MTRKR1141") }.exceptionOrNull()

        assertEquals("출발역과 도착역을 다르게 골라 주세요.", error?.toUserMessage())
        assertEquals(FavoriteStations("MTRKR1161", "MTRS11150"), repository.favoriteStations.value)
    }

    @Test
    fun 로그아웃하면_저장한_두_역도_잊는다() = runTest {
        val store = tokenStore(backgroundScope).apply { save(Tokens("A1", "R1")) }
        val repository = AccountRepository(api, store)
        server.enqueue(json(200, """{"id":1,"email":"me@example.com","originStationId":"MTRKR1161","destinationStationId":"MTRS11150"}"""))
        repository.getMe()
        server.enqueue(MockResponse.Builder().code(204).build())

        repository.logout()

        assertEquals(null, repository.favoriteStations.value)
    }

    private fun tokens(access: String, refresh: String) =
        """{"accessToken":"$access","refreshToken":"$refresh","tokenType":"Bearer","expiresIn":1800}"""

    private fun json(code: Int, body: String): MockResponse = MockResponse.Builder()
        .code(code)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()
}
