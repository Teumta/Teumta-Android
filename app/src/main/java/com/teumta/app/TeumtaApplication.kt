package com.teumta.app

import android.app.Application
import com.teumta.app.data.LocalTimetableRepository
import com.teumta.app.data.TimetableRepository

// 앱 전체에서 하나만 쓰는 객체를 둔다. 화면이 여러 개여도 시간표는 한 번만 읽는다
class TeumtaApplication : Application() {
    val timetableRepository: TimetableRepository by lazy { LocalTimetableRepository(this) }
}
