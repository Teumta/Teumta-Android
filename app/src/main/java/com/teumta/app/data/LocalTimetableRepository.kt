package com.teumta.app.data

import android.content.Context
import com.teumta.app.domain.model.Timetable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// app/src/main/assets/timetable.json을 한 번 읽어 메모리에 둔다.
// @Singleton이라 앱 전체에서 하나만 만들어지고, 입력·결과 화면이 같은 cache를 쓴다
@Singleton
class LocalTimetableRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : TimetableRepository {

    private val mutex = Mutex()
    private var cache: Timetable? = null

    override suspend fun getTimetable(): Timetable = mutex.withLock {
        cache ?: withContext(Dispatchers.IO) {
            val text = context.assets.open(FILE_NAME).bufferedReader().use { it.readText() }
            TimetableParser.parse(text)
        }.also { cache = it }
    }

    private companion object {
        const val FILE_NAME = "timetable.json"
    }
}
