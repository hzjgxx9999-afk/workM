package com.qkzc.workerm

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeToEdgeCompatibilityContractTest {

    @Test
    fun dispatchScreensUseTheSharedSafeAreaBaseline() {
        val activityFiles = listOf(
            "java/com/qkzc/workerm/ui/dispatch/DispatchListActivity.kt",
            "java/com/qkzc/workerm/ui/dispatch/DispatchCreateActivity.kt",
            "java/com/qkzc/workerm/ui/dispatch/DispatchDetailActivity.kt",
            "java/com/qkzc/workerm/ui/dispatch/SpotCheckReviewActivity.kt",
        )

        activityFiles.forEach { path ->
            val source = mainFile(path).readText()
            assertTrue("$path must use EdgeToEdgeActivity", source.contains(": EdgeToEdgeActivity()"))
            assertTrue("$path must apply insets", source.contains("applyEdgeToEdge("))
        }
    }

    @Test
    fun nonBusinessFullscreenScreensDeclareTheirInsetPolicy() {
        val launch = mainFile("java/com/qkzc/workerm/ui/auth/LaunchActivity.kt").readText()
        val preview = mainFile("java/com/qkzc/workerm/ui/dispatch/DispatchPhotoPreviewActivity.kt").readText()

        assertTrue(launch.contains(": EdgeToEdgeActivity()"))
        assertTrue(launch.contains("applyContentInsets("))
        assertTrue(preview.contains(": EdgeToEdgeActivity()"))
        assertTrue(preview.contains("photo_top_overlay"))
        assertTrue(preview.contains("applyEdgeToEdge("))
    }

    @Test
    fun dispatchListHasStableConstraintsInsteadOfPlatformSpinnerAndWeights() {
        val layout = mainFile("res/layout/activity_dispatch_list.xml").readText()

        assertTrue(layout.contains("androidx.constraintlayout.widget.ConstraintLayout"))
        assertTrue(layout.contains("@+id/status_filter_button"))
        assertTrue(layout.contains("app:layout_constraintBottom_toTopOf=\"@id/create_fab\""))
        assertFalse(layout.contains("<Spinner"))
        assertFalse(layout.contains("layout_weight"))
    }

    @Test
    fun safeAreaBaselineHandlesCutoutsNavigationAndIme() {
        val source = mainFile("java/com/qkzc/workerm/ui/common/EdgeToEdgeActivity.kt").readText()

        assertTrue(source.contains("WindowCompat.setDecorFitsSystemWindows(window, false)"))
        assertTrue(source.contains("Type.displayCutout()"))
        assertTrue(source.contains("Type.navigationBars()"))
        assertTrue(source.contains("Type.ime()"))
    }

    private fun mainFile(relativePath: String): File = sequenceOf(
        File("src/main/$relativePath"),
        File("app/src/main/$relativePath"),
    ).firstOrNull(File::isFile) ?: error("Missing main file: $relativePath")
}
