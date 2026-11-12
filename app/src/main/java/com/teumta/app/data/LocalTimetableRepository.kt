package com.teumta.app.data

import android.content.Context
import com.teumta.app.domain.model.Timetable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// app/src/main/assets/timetable.json 을 한 번 읽어 메모리에 둔다
class LocalTimetableRepository(private val context: Context) : TimetableRepository {

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