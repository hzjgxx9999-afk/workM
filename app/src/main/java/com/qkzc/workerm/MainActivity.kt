package com.qkzc.workerm

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.qkzc.workerm.data.session.AuthRepository
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityMainBinding
import com.qkzc.workerm.ui.auth.LoginActivity
import com.qkzc.workerm.ui.home.HomeFragment
import com.qkzc.workerm.ui.invite.InviteCodeManageFragment
import com.qkzc.workerm.ui.message.MessageFragment
import com.qkzc.workerm.ui.supervision.SupervisionFragment
import com.qkzc.workerm.ui.todo.TodoFragment
import com.qkzc.workerm.ui.worker.ProjectMemberManageFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            if (!hasValidSessionForMain()) {
                redirectToLogin()
                return@launch
            }
            setupMainContent(savedInstanceState)
        }
    }

    private suspend fun hasValidSessionForMain(): Boolean {
        val sessionStore = SessionStore(applicationContext)
        return if (intent.getBooleanExtra(EXTRA_SESSION_VALIDATED, false)) {
            sessionStore.sessionFlow.first().isLoggedIn
        } else {
            AuthRepository(sessionStore).restoreValidSession() != null
        }
    }

    private fun redirectToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        finish()
    }

    private fun setupMainContent(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom,
            )
            insets
        }
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            switchTab(item.itemId)
            true
        }
        supportFragmentManager.addOnBackStackChangedListener {
            binding.bottomNavigation.isVisible = supportFragmentManager.backStackEntryCount == 0
        }
        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    fun navigateToTab(itemId: Int) {
        binding.bottomNavigation.selectedItemId = itemId
    }

    fun openInviteCodeManage(projectId: Long? = null) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, InviteCodeManageFragment.newInstance(projectId), "invite_code_manage")
            addToBackStack("invite_code_manage")
        }
    }

    private fun switchTab(itemId: Int) {
        val tag = when (itemId) {
            R.id.nav_home -> "home"
            R.id.nav_supervision -> "supervision"
            R.id.nav_todo -> "todo"
            R.id.nav_message -> "message"
            R.id.nav_profile -> "approval"
            else -> "home"
        }
        val fragment = supportFragmentManager.findFragmentByTag(tag) ?: createFragment(itemId)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, fragment, tag)
        }
    }

    private fun createFragment(itemId: Int): Fragment {
        return when (itemId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_supervision -> SupervisionFragment()
            R.id.nav_todo -> ProjectMemberManageFragment.newInstance()
            R.id.nav_message -> MessageFragment()
            R.id.nav_profile -> TodoFragment()
            else -> HomeFragment()
        }
    }

    companion object {
        const val EXTRA_SESSION_VALIDATED = "com.qkzc.workerm.extra.SESSION_VALIDATED"
    }
}
