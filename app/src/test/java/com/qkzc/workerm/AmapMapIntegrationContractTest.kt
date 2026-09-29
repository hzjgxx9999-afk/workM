package com.qkzc.workerm

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AmapMapIntegrationContractTest {

    @Test
    fun mapKeySupportsExistingLocalConfigAndPrivacyIsInitializedBeforeMapViews() {
        val buildScript = mainFile("build.gradle.kts").readText()
        val initializer = mainFile("java/com/qkzc/workerm/data/privacy/AmapPrivacyInitializer.kt").readText()
        val picker = mainFile("java/com/qkzc/workerm/ui/dispatch/DispatchLocationPickerActivity.kt").readText()
        val geofence = mainFile("java/com/qkzc/workerm/ui/location/ProjectGeofenceActivity.kt").readText()

        assertTrue(buildScript.contains("AMAP_API_KEY"))
        assertTrue(buildScript.contains("amap.debug.api.key"))
        assertTrue(buildScript.contains("amap.release.api.key"))
        assertTrue(initializer.contains("MapsInitializer.updatePrivacyShow"))
        assertTrue(initializer.contains("MapsInitializer.updatePrivacyAgree"))
        assertTrue(initializer.contains("hasCurrentPolicyConsent"))
        assertTrue(picker.contains("AmapPrivacyInitializer.initializeIfConsented"))
        assertTrue(geofence.contains("AmapPrivacyInitializer.initializeIfConsented"))
    }

    @Test
    fun privacyPolicyDisclosesAmapMapSdk() {
        val legalDocuments = mainFile("java/com/qkzc/workerm/ui/legal/LegalDocuments.kt").readText()

        assertTrue(legalDocuments.contains("高德开放平台地图 SDK"))
        assertTrue(legalDocuments.contains("北京高德图强科技有限公司"))
        assertTrue(legalDocuments.contains("https://lbs.amap.com/pages/privacy/"))
    }

    private fun mainFile(relativePath: String): File = sequenceOf(
        File(relativePath),
        File("app/$relativePath"),
        File("src/main/$relativePath"),
        File("app/src/main/$relativePath"),
    ).firstOrNull(File::isFile) ?: error("Missing main file: $relativePath")
}
