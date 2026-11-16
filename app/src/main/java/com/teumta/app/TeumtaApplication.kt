package com.teumta.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Hilt가 앱 전체에서 쓸 객체 보관소(SingletonComponent)를 여기서 만든다
@HiltAndroidApp
class TeumtaApplication : Application()
