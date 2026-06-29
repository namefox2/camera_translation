plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.letsgo.translator.feature.ar"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:translation"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // CameraX (guava provides ListenableFuture used by ProcessCameraProvider)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.guava)

    // ML Kit Text Recognition — Play Services (unbundled) variants
    // Native .so lives in GMS, not bundled in APK → resolves 16 KB page-size warning
    implementation(libs.gms.mlkit.text.recognition)
    implementation(libs.gms.mlkit.text.recognition.chinese)
    implementation(libs.gms.mlkit.text.recognition.japanese)
    implementation(libs.gms.mlkit.text.recognition.korean)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // AdMob
    implementation(libs.play.services.ads)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)  // Task.await() for ML Kit
}
