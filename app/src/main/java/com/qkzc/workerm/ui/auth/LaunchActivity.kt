package com.qkzc.workerm.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.MainActivity
import com.qkzc.workerm.R
import com.qkzc.workerm.data.session.AuthRepository
import com.qkzc.workerm.data.session.SessionStore
import kotlinx.coroutines.launch

class LaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
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
