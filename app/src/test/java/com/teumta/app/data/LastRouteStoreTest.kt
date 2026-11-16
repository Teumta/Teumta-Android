package com.teumta.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.teumta.app.domain.model.LastRoute
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

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
    fun 다시_저장하면_마지막_값으로_바뀐다() = runTest {
        val store = store(backgroundScope)
        store.save(route)
        store.save(route.copy(viaId = "S20", stayMin = 10))
        assertEquals(route.copy(viaId = "S20", stayMin = 10), store.lastRoute.first())
    }
}
