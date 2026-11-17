package com.teumta.app.data

import com.teumta.app.domain.model.DayType
import com.teumta.app.domain.model.Station
import com.teumta.app.domain.model.Timetable
import com.teumta.app.domain.model.TimetableSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException

// 서버에서 시간표를 받고, 못 받으면 앱 안 시간표(bundled)를 쓴다.
// 서버가 꺼져 있거나 인터넷이 없어도 앱은 계산할 수 있어야 해서, 앱 안 시간표를 늘 같이 들고 있다
class RemoteTimetableRepository(
    private val api: TimetableApi,
    private val bundled: TimetableRepository,
) : TimetableRepository {

    private val mutex = Mutex()
    private var cache: Timetable? = null

    // 입력 화면은 앱 안 역 목록을 바로 쓴다(네트워크를 기다리지 않는다).
    // 서버 시간표는 이 역들과 같을 때만 쓰므로, 어느 시간표로 계산하든 고른 역이 들어 있다
    override suspend fun getStations(): List<Station> = bundled.getStations()

    // 앱을 켠 뒤 처음 한 번 서버에 묻고, 결과(서버든 앱 안이든)를 앱이 꺼질 때까지 쓴다
    override suspend fun getTimetable(): Timetable = mutex.withLock {
        cache ?: (fetchFromServer() ?: bundled.getTimetable()).also { cache = it }
    }

    // 역, 평일, 휴일을 동시에 받아 합친다. 못 받거나 앱이 아는 역과 다르면 null
    private suspend fun fetchFromServer(): Timetable? = try {
        val timetable = coroutineScope {
            val stations = async { api.getStations() }
            val days = DayType.entries.map { dayType -> async { api.getTrains(dayType) } }
            TimetableParser.combine(stations.await(), days.awaitAll())
        }
        val knownIds = bundled.getStations().map { it.id }.toSet()
        // 서버의 역이 바뀌었으면(새 역, 없어진 역) 화면에서 고른 역이 시간표에 없을 수 있어서 쓰지 않는다
        if (timetable.stations.map { it.id }.toSet() == knownIds) timetable.copy(source = TimetableSource.SERVER) else null
    } catch (e: IOException) {
        null // 서버가 꺼져 있음, 인터넷 없음, 시간 초과
    } catch (e: HttpException) {
        null // 서버가 오류 응답(404, 500 등)을 보냄
    } catch (e: IllegalArgumentException) {
        null // JSON 모양이 다름(SerializationException), 평일·휴일 기준일이 다름(require)
    }
}
