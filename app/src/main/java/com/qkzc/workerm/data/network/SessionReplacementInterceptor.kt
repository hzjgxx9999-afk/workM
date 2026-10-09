package com.qkzc.workerm.data.network

import android.content.Context
import com.qkzc.workerm.data.session.SessionReplacementLogoutCoordinator
import com.qkzc.workerm.data.session.SessionReplacementResponsePolicy
import com.qkzc.workerm.data.session.SessionInvalidationReason
import okhttp3.Interceptor
import okhttp3.Response

class SessionReplacementInterceptor(
    private val contextProvider: () -> Context,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.request.url.encodedPath == "/app/profile") return response
        val authorization = response.request.header("Authorization") ?: return response
        val contentType = response.body?.contentType()
        val mayContainAuthError = response.code == 401 ||
            contentType?.subtype?.contains("json", ignoreCase = true) == true
        val errorBody = if (mayContainAuthError && contentType?.subtype != "event-stream") {
            response.peekBody(16L * 1024L).string()
        } else {
            null
        }
        when (SessionReplacementResponsePolicy.invalidationReason(response.code, errorBody)) {
            SessionInvalidationReason.REPLACED -> {
                SessionReplacementLogoutCoordinator.logoutIfCurrentSessionWasReplaced(
                    contextProvider(),
                    authorization,
                )
            }
            SessionInvalidationReason.EXPIRED -> {
                SessionReplacementLogoutCoordinator.logoutIfCurrentSessionExpired(
                    contextProvider(),
                    authorization,
                )
            }
            null -> Unit
        }
        return response
    }
}
