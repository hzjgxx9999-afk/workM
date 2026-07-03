package com.qkzc.workerm

import com.qkzc.workerm.ui.login.shouldUseScrollableLoginLayout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupervisionLoginLayoutPolicyTest {

    @Test
    fun normalPhoneHeightUsesFixedSingleScreenLayout() {
        assertFalse(shouldUseScrollableLoginLayout(780))
        assertFalse(shouldUseScrollableLoginLayout(873))
    }

    @Test
    fun verySmallHeightKeepsScrollAsFallback() {
        assertTrue(shouldUseScrollableLoginLayout(640))
        assertTrue(shouldUseScrollableLoginLayout(560))
    }
}
