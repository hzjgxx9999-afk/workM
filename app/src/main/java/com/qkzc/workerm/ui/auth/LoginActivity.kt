package com.qkzc.workerm.ui.auth

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.qkzc.workerm.MainActivity
import com.qkzc.workerm.R
import com.qkzc.workerm.data.session.AuthRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.ui.login.SupervisionLoginRoute
import com.qkzc.workerm.ui.theme.WorkerMTheme

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModel.Factory(
            AuthRepository(SessionStore(applicationContext)),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()

        setContent {
            WorkerMTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState.errorMessage) {
                    uiState.errorMessage?.let { message ->
                        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                        viewModel.consumeError()
                    }
                }

                LaunchedEffect(uiState.loggedIn) {
                    if (uiState.loggedIn) {
                        Toast.makeText(
                            this@LoginActivity,
                            getString(R.string.login_success),
                            Toast.LENGTH_SHORT,
                        ).show()
                        viewModel.consumeLoggedIn()
                        startActivity(
                            Intent(this@LoginActivity, MainActivity::class.java)
                                .putExtra(MainActivity.EXTRA_SESSION_VALIDATED, true),
                        )
                        finish()
                    }
                }

                SupervisionLoginRoute(
                    loading = uiState.loading,
                    onLogin = { mobile, password ->
                        viewModel.login(mobile, password)
                    },
                    onPrivacyClick = {
                        Toast.makeText(this, "隐私说明待接入", Toast.LENGTH_SHORT).show()
                    },
                    onSecurityPolicyClick = {
                        Toast.makeText(this, "数据安全规范待接入", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.rgb(245, 249, 255)
        window.isNavigationBarContrastEnforced = false

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}
