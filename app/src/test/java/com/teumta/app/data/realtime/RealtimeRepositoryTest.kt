package com.teumta.app.data.realtime

import com.teumta.app.data.TimetableParser
import com.teumta.app.domain.model.TrainPosition
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.OffsetDateTime

// 가짜 서버(MockWebServer)에 11/21 23:00에 실제 틈타 서버가 준 응답을 돌려주게 하고,
// 맞는 주소로 묻는지, 응답을 앱 모델로 읽는지, 못 받으면 null(실시간 줄 숨김)인지 확인한다
class RealtimeRepositoryTest {

    private val server = MockWebServer()
    private lateinit var repository: RealtimeRepository

    @Before
    fun setUp() {
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(TimetableParser.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RealtimeApi::class.java)
        repository = RealtimeRepository(api)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun 탈_역과_내릴_역으로_묻고_가까운_순서대로_읽는다() = runTest {
        server.enqueue(json(resource("guro-to-guil.json")))

        val arrivals = repository.getArrivals("MTRKR1141", "MTRKR1142")!!

        assertEquals("/api/realtime/arrivals?stationId=MTRKR1141&towardStationId=MTRKR1142", server.takeRequest().target)
        assertEquals(6, arrivals.size)
        val first = arrivals.first()
        assertEquals("0209", first.trainNo)
        assertEquals("인천", first.terminal)
        assertEquals("영등포", first.currentStation)
        assertEquals(TrainPosition.ARRIVED, first.position)
        assertEquals(2, first.stationsAway)
        assertEquals(OffsetDateTime.parse("2026-11-21T22:58:04+09:00").toInstant(), first.receivedAt)
        assertTrue(arrivals[1].express)
    }

    @Test
    fun 오는_열차가_없으면_빈_목록() = runTest {
        // 23:00의 광명: 셔틀이 영등포로 가는 중이라 광명으로 오는 열차가 없다
        server.enqueue(json(resource("gwangmyeong.json")))

        assertEquals(emptyList<Any>(), repository.getArrivals("MTRKR1P144-1", "MTRKR1P144"))
    }

    @Test
    fun 서버가_503이면_null() = runTest {
        server.enqueue(
            MockResponse.Builder().code(503)
                .body("""{"code":"REALTIME_UNAVAILABLE","message":"실시간 도착 정보를 지금은 볼 수 없어요.","fieldErrors":[]}""")
                .build(),
        )

        assertNull(repository.getArrivals("MTRKR1141", "MTRKR1142"))
    }

    @Test
    fun 모양이_다른_응답이면_null() = runTest {
        server.enqueue(json("""{"oops":true}"""))

        assertNull(repository.getArrivals("MTRKR1141", "MTRKR1142"))
    }

    @Test
    fun 모르는_상태나_읽을_수_없는_시각인_줄만_버린다() = runTest {
        server.enqueue(
            json(
                """{"stationId":"MTRKR1141","fetchedAt":"2026-11-21T23:00:20+09:00","arrivals":[
                {"trainNo":"1","terminal":"인천","express":false,"currentStation":"신도림","currentStatus":"FLYING","stationsAway":1,"turnsBack":false,"receivedAt":"2026-11-21T23:00:00+09:00"},
                {"trainNo":"2","terminal":"인천","express":false,"currentStation":"신도림","currentStatus":"ARRIVED","stationsAway":1,"turnsBack":false,"receivedAt":"어제"},
                {"trainNo":"3","terminal":"인천","express":false,"currentStation":"신도림","currentStatus":"ENTERING","stationsAway":1,"turnsBack":false,"receivedAt":"2026-11-21T23:00:00+09:00"}]}""",
            ),
        )

        assertEquals(listOf("3"), repository.getArrivals("MTRKR1141", "MTRKR1142")!!.map { it.trainNo })
    }

    private fun json(body: String) = MockResponse.Builder()
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()

    private fun resource(name: String): String =
        javaClass.getResourceAsStream("/realtime/$name")!!.bufferedReader().use { it.readText() }
}
