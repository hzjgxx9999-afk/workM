package com.qkzc.workerm.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.qkzc.workerm.MainActivity
import com.qkzc.workerm.R
import com.qkzc.workerm.data.session.AuthRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityLoginBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModel.Factory(
            AuthRepository(SessionStore(applicationContext)),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.loginRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )
            insets
        }
        binding.accountInput.editText?.setText("13900010001")
        binding.passwordInput.editText?.setText("admin123")
        binding.loginButton.setOnClickListener {
            submitLogin()
        }
        observeUiState()
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    binding.loginButton.isEnabled = !state.loading
                    binding.loginButton.text = if (state.loading) {
                        getString(R.string.login_loading)
                    } else {
                        getString(R.string.login_submit)
                    }
                    state.errorMessage?.let { message ->
                        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                        viewModel.consumeError()
                    }
                    if (state.loggedIn) {
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
            }
        }
    }

    private fun submitLogin() {
        val mobile = binding.accountEdit.text?.toString().orEmpty().trim()
        val password = binding.passwordEdit.text?.toString().orEmpty().trim()

        binding.accountInput.error = null
        binding.passwordInput.error = null

        var hasError = false
        if (mobile.isBlank()) {
            binding.accountInput.error = getString(R.string.login_required_account)
            hasError = true
        }
        if (password.isBlank()) {
            binding.passwordInput.error = getString(R.string.login_required_password)
            hasError = true
        }
        if (hasError) return

        viewModel.login(mobile, password)
    }
}
