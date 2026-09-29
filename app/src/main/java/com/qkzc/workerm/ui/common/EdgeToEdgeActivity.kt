package com.qkzc.workerm.ui.common

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

/**
 * Shared edge-to-edge baseline for full-screen business screens.
 *
 * Android 15+ enforces edge-to-edge for apps targeting API 35 or higher, so every
 * screen using this base class must explicitly place its content around system UI.
 */
abstract class EdgeToEdgeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        configureSystemBarIconAppearance(
            lightStatusBars = false,
            lightNavigationBars = true,
        )
    }

    protected fun configureSystemBarIconAppearance(
        lightStatusBars: Boolean,
        lightNavigationBars: Boolean,
    ) {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightStatusBars
            isAppearanceLightNavigationBars = lightNavigationBars
        }
    }

    protected fun applyContentInsets(content: View) {
        val padding = content.paddingSnapshot()
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.navigationBars() or
                    WindowInsetsCompat.Type.displayCutout(),
            )
            view.updatePadding(
                left = padding.left + systemBars.left,
                top = padding.top + systemBars.top,
                right = padding.right + systemBars.right,
                bottom = padding.bottom + systemBars.bottom,
            )
            insets
        }
        ViewCompat.requestApplyInsets(content)
    }

    protected fun applyEdgeToEdge(
        root: View,
        @IdRes topBarId: Int,
        @IdRes bottomBarId: Int? = null,
        @IdRes floatingActionButtonId: Int? = null,
        @IdRes scrollContentId: Int? = null,
    ) {
        val topBar = root.findRequiredView(topBarId)
        val bottomBar: View? = bottomBarId?.let { root.findRequiredView(it) }
        val floatingActionButton: View? = floatingActionButtonId?.let { root.findRequiredView(it) }
        val scrollContent: View? = scrollContentId?.let { root.findRequiredView(it) }

        val topBarPadding = topBar.paddingSnapshot()
        val topBarHeight = topBar.layoutParams.height
        val bottomBarPadding = bottomBar?.paddingSnapshot()
        val fabMargins = floatingActionButton?.marginSnapshot()
        val scrollPadding = scrollContent?.paddingSnapshot()

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val topInset = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout(),
            ).top
            val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val imeBottomInset = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            topBar.updatePadding(
                left = topBarPadding.left + navigationInsets.left,
                top = topBarPadding.top + topInset,
                right = topBarPadding.right + navigationInsets.right,
                bottom = topBarPadding.bottom,
            )
            if (topBarHeight >= 0) {
                topBar.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = topBarHeight + topInset
                }
            }

            bottomBar?.let { bar ->
                val padding = bottomBarPadding ?: PaddingSnapshot.ZERO
                bar.updatePadding(
                    left = padding.left,
                    top = padding.top,
                    right = padding.right,
                    bottom = padding.bottom + maxOf(navigationInsets.bottom, imeBottomInset),
                )
            }

            floatingActionButton?.let { fab ->
                val margins = fabMargins ?: MarginSnapshot.ZERO
                fab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    leftMargin = margins.left + navigationInsets.left
                    rightMargin = margins.right + navigationInsets.right
                    bottomMargin = margins.bottom + navigationInsets.bottom
                }
            }

            scrollContent?.let { content ->
                val padding = scrollPadding ?: PaddingSnapshot.ZERO
                content.updatePadding(
                    left = padding.left + navigationInsets.left,
                    top = padding.top,
                    right = padding.right + navigationInsets.right,
                    bottom = padding.bottom + navigationInsets.bottom,
                )
            }
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun View.paddingSnapshot() = PaddingSnapshot(paddingLeft, paddingTop, paddingRight, paddingBottom)

    private fun View.findRequiredView(@IdRes viewId: Int): View =
        findViewById<View>(viewId) ?: error("Missing required view: $viewId")

    private fun View.marginSnapshot(): MarginSnapshot {
        val params = layoutParams as? ViewGroup.MarginLayoutParams ?: return MarginSnapshot.ZERO
        return MarginSnapshot(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin)
    }

    private data class PaddingSnapshot(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        companion object {
            val ZERO = PaddingSnapshot(0, 0, 0, 0)
        }
    }

    private data class MarginSnapshot(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        companion object {
            val ZERO = MarginSnapshot(0, 0, 0, 0)
        }
    }
}
