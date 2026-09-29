package com.qkzc.workerm.ui.auth

import android.graphics.Color
import android.content.Intent
import android.app.AlertDialog
import android.os.Build
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
import com.qkzc.workerm.data.privacy.PrivacyConsentStore
import com.qkzc.workerm.data.session.AuthRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.data.session.SessionReplacementLogoutCoordinator
import com.qkzc.workerm.ui.legal.LegalDocumentActivity
import com.qkzc.workerm.ui.legal.LegalDocumentType
import com.qkzc.workerm.ui.login.SupervisionLoginRoute
import com.qkzc.workerm.ui.theme.WorkerMTheme

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModel.Factory(
            AuthRepository(SessionStore(applicationContext)),
            PrivacyConsentStore(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()

        if (intent.getBooleanExtra(SessionReplacementLogoutCoordinator.EXTRA_SESSION_REPLACED, false)) {
            window.decorView.post {
                AlertDialog.Builder(this)
                    .setTitle(R.string.session_replaced_title)
                    .setMessage(R.string.session_replaced_message)
                    .setPositiveButton(R.string.session_replaced_confirm, null)
                    .setCancelable(false)
                    .show()
            }
        }

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
                    onServiceAgreementClick = {
                        startActivity(
                            LegalDocumentActivity.createIntent(
                                this,
                                LegalDocumentType.SERVICE_AGREEMENT,
                            ),
                        )
                    },
                    onPrivacyClick = {
                        startActivity(
                            LegalDocumentActivity.createIntent(
                                this,
                                LegalDocumentType.PRIVACY_POLICY,
                            ),
                        )
                    },
                )
            }
        }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.rgb(245, 249, 255)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}
