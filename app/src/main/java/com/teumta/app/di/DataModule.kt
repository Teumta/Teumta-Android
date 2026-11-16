package com.teumta.app.di

import com.teumta.app.data.LocalTimetableRepository
import com.teumta.app.data.TimetableRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// "TimetableRepository를 달라고 하면 LocalTimetableRepository를 준다"는 연결표.
// 서버를 붙일 때는 여기 한 줄만 RemoteTimetableRepository로 바꾸면 된다
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindTimetableRepository(impl: LocalTimetableRepository): TimetableRepository
}
