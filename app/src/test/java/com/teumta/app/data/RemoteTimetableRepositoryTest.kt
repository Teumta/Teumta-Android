package com.teumta.app.data

import com.teumta.app.domain.model.DayType
import com.teumta.app.domain.model.Station
import com.teumta.app.domain.model.Timetable
import com.teumta.app.domain.model.TimetableSource
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.IOException

class RemoteTimetableRepositoryTest {

    // 앱 안 시간표 자리에 102개 역 시간표를 그대로 넣는다
    private val bundled = object : TimetableRepository {
        override suspend fun getStations(): List<Station> = Line1Assets.timetable.stations
        override suspend fun getTimetable(): Timetable = Line1Assets.timetable
    }

    @Test
    fun 서버에서_받으면_서버_시간표를_쓴다() = runTest {
        val api = FakeApi()
        val repository = RemoteTimetableRepository(api, bundled)

        val timetable = repository.getTimetable()

        assertEquals(TimetableSource.SERVER, timetable.source)
        assertEquals(1574, timetable.trains.size)
        // 두 번째부터는 다시 묻지 않는다 (역 1번 + 평일·휴일 2번)
        repository.getTimetable()
        assertEquals(3, api.calls)
    }

    @Test
    fun 서버에_못_붙으면_앱_안_시간표를_쓴다() = runTest {
        val repository = RemoteTimetableRepository(FakeApi(fail = IOException("연결 거부")), bundled)
        assertEquals(TimetableSource.BUNDLED, repository.getTimetable().source)
    }

    @Test
    fun 서버가_오류를_보내도_앱_안_시간표를_쓴다() = runTest {
        val error = HttpException(Response.error<Any>(500, "".toResponseBody()))
        val repository = RemoteTimetableRepository(FakeApi(fail = error), bundled)
        assertEquals(TimetableSource.BUNDLED, repository.getTimetable().source)
    }

    @Test
    fun 서버의_역이_앱과_다르면_앱_안_시간표를_쓴다() = runTest {
        val fewer = Line1Assets.timetable.stations.drop(1)
        val repository = RemoteTimetableRepository(FakeApi(stations = fewer), bundled)
        assertEquals(TimetableSource.BUNDLED, repository.getTimetable().source)
    }

    // 실제 서버 대신 assets의 응답을 돌려주거나, 정해 둔 예외를 던진다
    private class FakeApi(
        private val fail: Exception? = null,
        private val stations: List<Station> = Line1Assets.timetable.stations,
    ) : TimetableApi {
        var calls = 0

        override suspend fun getStations(): List<Station> {
            calls++
            fail?.let { throw it }
            return stations
        }

        override suspend fun getTrains(dayType: DayType): TrainsResponse {
            calls++
            fail?.let { throw it }
            return TimetableParser.parseTrains(File("src/main/assets/line1/trains-${dayType.name.lowercase()}.json").readText())
        }
    }
}
