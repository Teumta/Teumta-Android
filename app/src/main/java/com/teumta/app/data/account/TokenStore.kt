package com.teumta.app.data.account

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

// 로그인 토큰 한 쌍
data class Tokens(val accessToken: String, val refreshToken: String)

// 로그인 토큰을 기기(DataStore)에 저장한다. 앱을 다시 열어도 로그인이 이어진다.
// DataStore 파일(datastore/)은 백업 규칙에서 빼 두어서 Google 백업이나 새 휴대폰으로 옮겨지지 않는다(개인정보처리방침 1장)
@Singleton
class TokenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    // 저장한 토큰. 로그인하지 않았거나 로그아웃했으면 null
    val tokens: Flow<Tokens?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            Tokens(
                accessToken = prefs[ACCESS_TOKEN] ?: return@map null,
                refreshToken = prefs[REFRESH_TOKEN] ?: return@map null,
            )
        }

    // 두 토큰을 한 번에 바꾼다. 한쪽만 새것이 되는 일이 없다
    suspend fun save(tokens: Tokens) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = tokens.accessToken
            prefs[REFRESH_TOKEN] = tokens.refreshToken
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(REFRESH_TOKEN)
        }
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }
}
