package com.teumta.app.data

import android.content.Context
import com.teumta.app.domain.model.Station
import com.teumta.app.domain.model.Timetable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// app/src/main/assets/line1의 시간표(서버 응답을 그대로 저장한 역·평일·휴일 파일 세 개)를 읽어 메모리에 둔다.
// 역(4KB)과 열차(약 2.8MB)를 따로 읽는다. 열차는 읽는 데 오래 걸려서(출시 빌드 약 1초), 입력 화면이 역만 먼저 받아 바로 뜨게 한다.
// @Singleton이라 앱 전체에서 하나만 만들어지고, 입력·결과 화면이 같은 cache를 쓴다
@Singleton
class LocalTimetableRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : TimetableRepository {

    // getTimetable 안에서 getStations를 부르므로, 같은 Mutex를 두 번 잠그지 않게 따로 둔다
    private val stationsMutex = Mutex()
    private val timetableMutex = Mutex()
    private var stations: List<Station>? = null
    private var timetable: Timetable? = null

    override suspend fun getStations(): List<Station> = stationsMutex.withLock {
        stations ?: withContext(Dispatchers.IO) {
            TimetableParser.parseStations(read("stations.json"))
        }.also { stations = it }
    }

    override suspend fun getTimetable(): Timetable {
        val stations = getStations()
        return timetableMutex.withLock {
            timetable ?: withContext(Dispatchers.IO) {
                // 평일·휴일 파일을 동시에 읽어 기다리는 시간을 줄인다
                val days = TRAIN_FILES.map { name -> async { TimetableParser.parseTrains(read(name)) } }.awaitAll()
                TimetableParser.combine(stations, days)
            }.also { timetable = it }
        }
    }

    private fun read(name: String): String =
        context.assets.open("$FOLDER/$name").bufferedReader().use { it.readText() }

    private companion object {
        const val FOLDER = "line1"
        val TRAIN_FILES = listOf("trains-weekday.json", "trains-holiday.json")
    }
}
