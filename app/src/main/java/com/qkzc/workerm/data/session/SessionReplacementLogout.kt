package com.qkzc.workerm.data.session

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.google.gson.JsonParser
import com.qkzc.workerm.ui.auth.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean

/** Matches the server response that means a newer device has replaced this login session. */
object SessionReplacementResponsePolicy {
    const val HTTP_UNAUTHORIZED = 401
    const val SESSION_REPLACED_CODE = 4601

    fun isSessionReplaced(httpCode: Int, rawBody: String?): Boolean {
        if (httpCode != HTTP_UNAUTHORIZED || rawBody.isNullOrBlank()) return false
        val body = runCatching { JsonParser.parseString(rawBody).asJsonObject }.getOrNull() ?: return false
        return body.get("code")?.asInt == SESSION_REPLACED_CODE ||
            body.get("reason")?.asString == "SESSION_REPLACED"
    }
}

/** Clears only the session whose token was rejected, then shows the replacement notice on login. */
object SessionReplacementLogoutCoordinator {
    const val EXTRA_SESSION_REPLACED = "com.qkzc.workerm.extra.SESSION_REPLACED"

    private val logoutStarted = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun logoutIfCurrentSessionWasReplaced(context: Context, failedAuthorization: String?): Boolean {
        val appContext = context.applicationContext
        val failedToken = normalizeToken(failedAuthorization)
        val currentToken = runBlocking { SessionStore(appContext).sessionFlow.first().accessToken }
        if (failedToken.isNullOrBlank() || failedToken != normalizeToken(currentToken)) return false
        if (!logoutStarted.compareAndSet(false, true)) return true

        scope.launch {
            AuthRepository(SessionStore(appContext)).logout()
            mainHandler.post {
                appContext.startActivity(
                    Intent(appContext, LoginActivity::class.java)
                        .putExtra(EXTRA_SESSION_REPLACED, true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                )
            }
        }
        return true
    }

    fun onSuccessfulLogin() {
        logoutStarted.set(false)
    }

    private fun normalizeToken(value: String?): String? = value
        ?.trim()
        ?.removePrefix("Bearer ")
        ?.removePrefix("bearer ")
        ?.takeIf { it.isNotBlank() }
}
