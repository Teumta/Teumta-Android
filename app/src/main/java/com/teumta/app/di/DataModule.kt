package com.teumta.app.di

import com.teumta.app.data.LocalTimetableRepository
import com.teumta.app.data.RemoteTimetableRepository
import com.teumta.app.data.TimetableApi
import com.teumta.app.data.TimetableRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// "TimetableRepository를 달라고 하면 무엇을 줄지" 정하는 연결표.
// 서버에서 받는 RemoteTimetableRepository를 주고, 그 안에 앱 안 시간표(LocalTimetableRepository)를 넣어 둔다
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    // RemoteTimetableRepository는 TimetableRepository를 하나 더 받는다(앱 안 시간표).
    // @Binds로는 "어느 TimetableRepository를 넣을지"를 정할 수 없어서, 여기서 직접 만들어 넘긴다
    @Provides
    @Singleton
    fun provideTimetableRepository(api: TimetableApi, local: LocalTimetableRepository): TimetableRepository =
        RemoteTimetableRepository(api, bundled = local)
}
