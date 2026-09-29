package com.qkzc.workerm.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.MainActivity
import com.qkzc.workerm.R
import com.qkzc.workerm.data.session.AuthRepository
import com.qkzc.workerm.data.session.SessionStore
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
            val restoredSession = AuthRepository(sessionStore).restoreValidSession()
            val target = when (resolveRestoredLaunchDestination(restoredSession)) {
                LaunchDestination.LOGIN -> LoginActivity::class.java
                LaunchDestination.MAIN -> MainActivity::class.java
            }
            val intent = Intent(this@LaunchActivity, target)
            if (target == MainActivity::class.java) {
                intent.putExtra(MainActivity.EXTRA_SESSION_VALIDATED, true)
            }
            startActivity(intent)
            finish()
        }
    }
}
