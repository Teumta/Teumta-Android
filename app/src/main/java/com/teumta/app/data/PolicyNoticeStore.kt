package com.teumta.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// 개인정보처리방침이 바뀌었다는 안내를 어느 판까지 봤는지 기기에 적어 둔다.
// 0.2.0 방침 7장에 "처리하는 정보가 달라지면 앱에서도 알려요"라고 약속해서, 새 판마다 한 번씩 보여 준다
@Singleton
class PolicyNoticeStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    // 마지막으로 확인한 방침 판(예: "0.3.0"). 본 적이 없으면 null
    val seenVersion: Flow<String?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[SEEN_VERSION] }

    suspend fun markSeen(version: String) {
        dataStore.edit { it[SEEN_VERSION] = version }
    }

    private companion object {
        val SEEN_VERSION = stringPreferencesKey("policy_notice_seen_version")
    }
}
