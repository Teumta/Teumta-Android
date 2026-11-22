package com.teumta.app.ui.update

import com.google.android.play.core.install.model.UpdateAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateRulesTest {

    @Test
    fun 새_버전이_있을_때만_묻는다() {
        assertTrue(shouldOfferUpdate(UpdateAvailability.UPDATE_AVAILABLE))
        assertFalse(shouldOfferUpdate(UpdateAvailability.UPDATE_NOT_AVAILABLE))
        assertFalse(shouldOfferUpdate(UpdateAvailability.UNKNOWN))
        // 이미 업데이트하는 중이면 묻지 않고 이어서 한다
        assertFalse(shouldOfferUpdate(UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS))
    }

    @Test
    fun Play_스토어_앱을_먼저_열고_없으면_웹_주소() {
        assertEquals(
            listOf(
                "market://details?id=com.teumta.app",
                "https://play.google.com/store/apps/details?id=com.teumta.app",
            ),
            playStoreUris("com.teumta.app"),
        )
    }
}
