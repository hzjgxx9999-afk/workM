package com.qkzc.workerm.ui.auth

import com.qkzc.workerm.data.session.LoginSession

enum class LaunchDestination {
    LOGIN,
    MAIN,
}

fun resolveLaunchDestination(session: LoginSession): LaunchDestination {
    return if (session.isLoggedIn) {
        LaunchDestination.MAIN
    } else {
        LaunchDestination.LOGIN
    }
}

fun resolveRestoredLaunchDestination(restoredSession: LoginSession?): LaunchDestination {
    return if (restoredSession != null) {
        LaunchDestination.MAIN
    } else {
        LaunchDestination.LOGIN
    }
}
