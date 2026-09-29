package com.qkzc.workerm.data.network

import android.content.Context
import com.qkzc.workerm.data.session.SessionReplacementLogoutCoordinator
import com.qkzc.workerm.data.session.SessionReplacementResponsePolicy
import okhttp3.Interceptor
import okhttp3.Response

class SessionReplacementInterceptor(
    private val contextProvider: () -> Context,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (SessionReplacementResponsePolicy.isSessionReplaced(
                response.code,
                response.peekBody(16L * 1024L).string(),
            )
        ) {
            SessionReplacementLogoutCoordinator.logoutIfCurrentSessionWasReplaced(
                contextProvider(),
                response.request.header("Authorization"),
            )
        }
        return response
    }
}
