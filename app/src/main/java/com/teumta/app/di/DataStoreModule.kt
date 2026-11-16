package com.teumta.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// DataStore는 생성자에 @Inject를 붙일 수 없다. 그래서 만드는 방법을 @Provides로 알려준다
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    // 같은 파일에 DataStore가 둘 이상 생기면 안 되므로 @Singleton
    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = {
            context.preferencesDataStoreFile("user_prefs")
        })
}
