import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val amapPropertiesFile = rootProject.file("amap.properties")
val amapProperties = Properties().apply {
    if (amapPropertiesFile.exists()) amapPropertiesFile.inputStream().use(::load)
}

/**
 * `AMAP_API_KEY` is retained as the compatible local-development key name.
 * New environments should provide independent debug/release keys where possible.
 */
fun Properties.requireAmapKey(vararg propertyNames: String): String =
    propertyNames.asSequence()
        .map { propertyName -> getProperty(propertyName).orEmpty().trim() }
        .firstOrNull { key -> key.isNotEmpty() }
        ?: error(
            "Missing AMap API key. Configure one of: ${propertyNames.joinToString()} in amap.properties.",
        )

val amapDebugApiKey = amapProperties.requireAmapKey(
    "amap.debug.api.key",
    "AMAP_API_KEY",
)
val amapReleaseApiKey = amapProperties.requireAmapKey(
    "amap.release.api.key",
    "amap.debug.api.key",
    "AMAP_API_KEY",
)

android {
    namespace = "com.qkzc.workerm"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.qkzc.workerm"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val supervisorBaseUrl = providers.gradleProperty("SUPERVISOR_BASE_URL")
            .orElse("http://192.168.3.166:8080/")
            .get()
        val cadPreviewBaseUrl = providers.gradleProperty("CAD_PREVIEW_BASE_URL")
            .orElse("http://192.168.3.166:5173/cad-preview.html")
            .get()
        val defaultProjectId = providers.gradleProperty("DEFAULT_PROJECT_ID")
            .orElse("1")
            .get()
        buildConfigField("String", "SUPERVISOR_BASE_URL", "\"$supervisorBaseUrl\"")
        buildConfigField("String", "CAD_PREVIEW_BASE_URL", "\"$cadPreviewBaseUrl\"")
        buildConfigField("long", "DEFAULT_PROJECT_ID", "${defaultProjectId}L")
        manifestPlaceholders["AMAP_LOCATION_API_KEY"] = amapDebugApiKey
    }

    buildTypes {
        debug {
            manifestPlaceholders["AMAP_LOCATION_API_KEY"] = amapDebugApiKey
        }
        release {
            manifestPlaceholders["AMAP_LOCATION_API_KEY"] = amapReleaseApiKey
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.google.mlkit.barcode)
    implementation(libs.coil)
    implementation("com.amap.api:3dmap-location-search:11.2.000_loc11.2.000_sea9.8.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
