package com.qkzc.workerm.ui.invite

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import com.qkzc.workerm.R
import com.qkzc.workerm.databinding.ActivityInviteCodeManageBinding

class InviteCodeManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInviteCodeManageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityInviteCodeManageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.inviteCodeRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )
            insets
        }
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(
                    R.id.invite_code_container,
                    InviteCodeManageFragment.newInstance(intent.getLongExtra(EXTRA_PROJECT_ID, 0L).takeIf { it > 0L }),
                )
            }
        }
    }

    companion object {
        const val EXTRA_PROJECT_ID = "projectId"
    }
}
