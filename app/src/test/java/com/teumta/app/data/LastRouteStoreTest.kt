package com.teumta.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.teumta.app.domain.model.LastRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

// DataStore는 파일만 있으면 되어서 에뮬레이터 없이 임시 폴더로 확인한다.
class LastRouteStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun store(scope: CoroutineScope) = LastRouteStore(
        PreferenceDataStoreFactory.create(scope = scope, produceFile = {
            File(
                folder.root,
                "test.preferences_pb"
            )
        }),
    )

    private val route = LastRoute(fromId = "S1", viaId = "S9", toId = "S29", stayMin = 60)

    @Test
    fun 저장한_적이_없으면_null() = runTest {
        assertNull(store(backgroundScope).lastRoute.first())
    }

    @Test
    fun 저장하면_다시_읽힌다() = runTest {
        val store = store(backgroundScope)
        store.save(route)
        assertEquals(route, store.lastRoute.first())
    }

    @Test
    fun 들르지_않은_경로는_들를_역_없이_다시_읽힌다() = runTest {
        val store = store(backgroundScope)
        store.save(route)
        // 바로 가기로 다시 저장하면 지난번 들를 역이 남지 않는다
        store.save(route.copy(viaId = null))
        assertEquals(route.copy(viaId = null), store.lastRoute.first())
    }

    @Test
    fun 다시_저장하면_마지막_값으로_바뀐다() = runTest {
        val store = store(backgroundScope)
        store.save(route)
        store.save(route.copy(viaId = "S20", stayMin = 10))
        assertEquals(route.copy(viaId = "S20", stayMin = 10), store.lastRoute.first())
    }
}
