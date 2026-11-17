package com.teumta.app.data

import com.teumta.app.domain.model.DayType
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File

// 가짜 서버(MockWebServer)에 앱 안 assets/line1 파일(= 실제 서버 응답)을 돌려주게 하고,
// TimetableApi가 맞는 주소로 요청하고 응답을 앱 모델로 읽는지 확인한다
class TimetableApiTest {

    private val server = MockWebServer()
    private lateinit var api: TimetableApi

    @Before
    fun setUp() {
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(TimetableParser.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TimetableApi::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun 역_목록을_받는다() = runTest {
        server.enqueue(json("stations.json"))

        val stations = api.getStations()

        assertEquals("/api/stations", server.takeRequest().target)
        assertEquals(102, stations.size)
        // 앱 모델에 없는 칸(branch, expressStop)은 건너뛰고 parentId, transferSec는 읽는다
        assertEquals(240, stations.single { it.name == "구로" }.transferSec)
    }

    @Test
    fun 요일_종류를_붙여_열차를_받는다() = runTest {
        server.enqueue(json("trains-holiday.json"))

        val trains = api.getTrains(DayType.HOLIDAY)

        assertEquals("/api/trains?dayType=HOLIDAY", server.takeRequest().target)
        assertEquals(DayType.HOLIDAY, trains.dayType)
        assertEquals(731, trains.trains.size)
    }

    private fun json(name: String): MockResponse = MockResponse.Builder()
        .addHeader("Content-Type", "application/json")
        .body(File("src/main/assets/line1/$name").readText())
        .build()
}
