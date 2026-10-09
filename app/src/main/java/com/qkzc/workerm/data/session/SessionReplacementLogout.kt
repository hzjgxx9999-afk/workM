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
import java.util.concurrent.atomic.AtomicBoolean

/** Matches the server response that means a newer device has replaced this login session. */
object SessionReplacementResponsePolicy {
    const val HTTP_UNAUTHORIZED = 401
    const val SESSION_REPLACED_CODE = 4601

    fun invalidationReason(httpCode: Int, rawBody: String?): SessionInvalidationReason? {
        val body = rawBody?.takeIf { it.isNotBlank() }?.let {
            runCatching { JsonParser.parseString(it).asJsonObject }.getOrNull()
        }
        val code = runCatching { body?.get("code")?.asInt }.getOrNull()
        val reason = runCatching { body?.get("reason")?.asString }.getOrNull()
        if (code == SESSION_REPLACED_CODE || reason == "SESSION_REPLACED") {
            return SessionInvalidationReason.REPLACED
        }
        if (httpCode == HTTP_UNAUTHORIZED || code == HTTP_UNAUTHORIZED) {
            return SessionInvalidationReason.EXPIRED
        }
        return null
    }

    fun isSessionReplaced(httpCode: Int, rawBody: String?): Boolean =
        invalidationReason(httpCode, rawBody) == SessionInvalidationReason.REPLACED
}

enum class SessionInvalidationReason {
    EXPIRED,
    REPLACED,
}

/** Clears only the session whose token was rejected, then shows the replacement notice on login. */
object SessionReplacementLogoutCoordinator {
    const val EXTRA_SESSION_REPLACED = "com.qkzc.workerm.extra.SESSION_REPLACED"
    const val EXTRA_SESSION_EXPIRED = "com.qkzc.workerm.extra.SESSION_EXPIRED"

    private val logoutStarted = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var startupValidationInProgress = false

    fun setStartupValidationInProgress(inProgress: Boolean) {
        startupValidationInProgress = inProgress
    }

    fun logoutIfCurrentSessionWasReplaced(context: Context, failedAuthorization: String?): Boolean =
        invalidateCurrentSession(context, failedAuthorization, SessionInvalidationReason.REPLACED)

    fun logoutIfCurrentSessionExpired(context: Context, failedAuthorization: String?): Boolean =
        invalidateCurrentSession(context, failedAuthorization, SessionInvalidationReason.EXPIRED)

    private fun invalidateCurrentSession(
        context: Context,
        failedAuthorization: String?,
        reason: SessionInvalidationReason,
    ): Boolean {
        if (startupValidationInProgress) return false
        val appContext = context.applicationContext
        val failedToken = normalizeToken(failedAuthorization)
        if (failedToken.isNullOrBlank()) return false
        if (!logoutStarted.compareAndSet(false, true)) return true

        scope.launch {
            try {
                val sessionStore = SessionStore(appContext)
                val currentToken = sessionStore.sessionFlow.first().accessToken
                if (failedToken != normalizeToken(currentToken)) {
                    logoutStarted.set(false)
                    return@launch
                }
                if (!sessionStore.clearSessionIfAccessTokenMatches(currentToken)) {
                    logoutStarted.set(false)
                    return@launch
                }
                mainHandler.post {
                    if (!logoutStarted.get()) return@post
                    appContext.startActivity(
                        Intent(appContext, LoginActivity::class.java)
                            .putExtra(EXTRA_SESSION_REPLACED, reason == SessionInvalidationReason.REPLACED)
                            .putExtra(EXTRA_SESSION_EXPIRED, reason == SessionInvalidationReason.EXPIRED)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    )
                }
            } catch (_: Exception) {
                logoutStarted.set(false)
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
