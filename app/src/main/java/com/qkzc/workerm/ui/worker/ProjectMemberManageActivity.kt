package com.qkzc.workerm.ui.worker

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import com.qkzc.workerm.R
import com.qkzc.workerm.databinding.ActivityProjectMemberManageBinding

class ProjectMemberManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProjectMemberManageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProjectMemberManageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.projectMemberRoot) { view, insets ->
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
                    R.id.project_member_container,
                    ProjectMemberManageFragment.newInstance(
                        intent.getLongExtra(EXTRA_PROJECT_ID, 0L).takeIf { it > 0L },
                    ),
                )
            }
        }
    }

    companion object {
        const val EXTRA_PROJECT_ID = "projectId"
    }
}
