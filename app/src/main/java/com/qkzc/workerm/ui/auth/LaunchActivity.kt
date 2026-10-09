package com.qkzc.workerm.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.MainActivity
import com.qkzc.workerm.R
import com.qkzc.workerm.data.session.AuthRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.data.session.SessionInvalidationReason
import com.qkzc.workerm.data.session.SessionReplacementLogoutCoordinator
import com.qkzc.workerm.ui.common.EdgeToEdgeActivity
import kotlinx.coroutines.launch

class LaunchActivity : EdgeToEdgeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
        configureSystemBarIconAppearance(
            lightStatusBars = true,
            lightNavigationBars = true,
        )
        applyContentInsets(findViewById(R.id.launch_root))
        lifecycleScope.launch {
            val sessionStore = SessionStore(applicationContext)
            val authRepository = AuthRepository(sessionStore)
            SessionReplacementLogoutCoordinator.setStartupValidationInProgress(true)
            val restoredSession = try {
                authRepository.restoreValidSession()
            } finally {
                SessionReplacementLogoutCoordinator.setStartupValidationInProgress(false)
            }
            val target = when (resolveRestoredLaunchDestination(restoredSession)) {
                LaunchDestination.LOGIN -> LoginActivity::class.java
                LaunchDestination.MAIN -> MainActivity::class.java
            }
            val intent = Intent(this@LaunchActivity, target)
            if (target == MainActivity::class.java) {
                intent.putExtra(MainActivity.EXTRA_SESSION_VALIDATED, true)
            }
            when (authRepository.lastSessionInvalidationReason) {
                SessionInvalidationReason.REPLACED -> intent.putExtra(
                    SessionReplacementLogoutCoordinator.EXTRA_SESSION_REPLACED,
                    true,
                )
                SessionInvalidationReason.EXPIRED -> intent.putExtra(
                    SessionReplacementLogoutCoordinator.EXTRA_SESSION_EXPIRED,
                    true,
                )
                null -> Unit
            }
            startActivity(intent)
            finish()
        }
    }
}
