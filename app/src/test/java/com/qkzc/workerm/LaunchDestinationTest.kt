package com.qkzc.workerm

import com.qkzc.workerm.data.session.LoginSession
import com.qkzc.workerm.ui.auth.LaunchDestination
import com.qkzc.workerm.ui.auth.resolveLaunchDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchDestinationTest {

    @Test
    fun emptyTokenRoutesToLogin() {
        assertEquals(
            LaunchDestination.LOGIN,
            resolveLaunchDestination(LoginSession(accessToken = "")),
        )
    }

    @Test
    fun savedTokenRoutesToMain() {
        assertEquals(
            LaunchDestination.MAIN,
            resolveLaunchDestination(LoginSession(accessToken = "token-123")),
        )
    }
}
