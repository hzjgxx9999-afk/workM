package com.qkzc.workerm

import com.qkzc.workerm.data.session.SessionReplacementResponsePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionReplacementResponsePolicyTest {

    @Test
    fun replacementCodeWithUnauthorizedResponseIsRecognized() {
        assertTrue(SessionReplacementResponsePolicy.isSessionReplaced(401, "{\"code\":4601}"))
    }

    @Test
    fun replacementReasonWithUnauthorizedResponseIsRecognized() {
        assertTrue(SessionReplacementResponsePolicy.isSessionReplaced(401, "{\"reason\":\"SESSION_REPLACED\"}"))
    }

    @Test
    fun ordinaryUnauthorizedAndNonUnauthorizedResponsesAreNotMisclassified() {
        assertFalse(SessionReplacementResponsePolicy.isSessionReplaced(401, "{\"code\":401}"))
        assertFalse(SessionReplacementResponsePolicy.isSessionReplaced(403, "{\"code\":4601}"))
    }
}
