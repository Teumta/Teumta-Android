package com.teumta.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teumta.app.domain.model.LastRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// 마지막으로 계산한 경로를 기기(DataStore)에 저장한다. 앱을 다시 열거나 프로세스가 종료돼도 남는다
@Singleton
class LastRouteStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    // 저장한 값이 바뀔 때마다 새 값을 내보내는 흐름. 들를 역 말고 하나라도 비어 있으면 null
    val lastRoute: Flow<LastRoute?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e } // 파일을 못 읽으면 빈 값으로
        .map { prefs ->
            LastRoute(
                fromId = prefs[FROM_ID] ?: return@map null,
                // 들를 역이 없으면(바로 가기) 비어 있다
                viaId = prefs[VIA_ID],
                toId = prefs[TO_ID] ?: return@map null,
                stayMin = prefs[STAY_MIN] ?: return@map null,
            )
        }

    // edit 안의 변경은 한 번에 저장된다(트랜잭션). 역 3개 중 일부만 저장되는 일이 없다.
    suspend fun save(route: LastRoute) {
        dataStore.edit { prefs ->
            prefs[FROM_ID] = route.fromId
            // 바로 가기면 지난번 들를 역이 남지 않게 지운다
            if (route.viaId != null) prefs[VIA_ID] = route.viaId else prefs.remove(VIA_ID)
            prefs[TO_ID] = route.toId
            prefs[STAY_MIN] = route.stayMin
        }
    }

    private companion object {
        val FROM_ID = stringPreferencesKey("last_from_id")
        val VIA_ID = stringPreferencesKey("last_via_id")
        val TO_ID = stringPreferencesKey("last_to_id")
        val STAY_MIN = intPreferencesKey("last_stay_min")
    }
}
