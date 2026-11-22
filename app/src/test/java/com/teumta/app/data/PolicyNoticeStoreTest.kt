package com.teumta.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.teumta.app.ui.info.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

// 바뀐 방침 안내를 본 판을 기억하는지. 판이 다르면 다시 보여 주는 판단은 InputViewModel이 한다
class PolicyNoticeStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun store(scope: CoroutineScope) = PolicyNoticeStore(
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(folder.root, "test.preferences_pb") }),
    )

    @Test
    fun 처음에는_본_판이_없다() = runTest {
        assertNull(store(backgroundScope).seenVersion.first())
    }

    @Test
    fun 확인하면_그_판을_기억한다() = runTest {
        val store = store(backgroundScope)
        store.markSeen(AppInfo.PRIVACY_VERSION)
        assertEquals("0.3.1", store.seenVersion.first())
    }
}
