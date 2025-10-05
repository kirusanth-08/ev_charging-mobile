plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

import java.util.Properties

android {
    namespace = "com.example.evcharger"
    compileSdk = 36

    // Read per-machine API base URL from local.properties (not committed)
    val localProps = Properties().apply {
        val lp = rootProject.file("local.properties")
        if (lp.exists()) load(lp.inputStream())
    }
    val apiBaseUrl: String = (localProps.getProperty("api.base.url")
        ?: "http://10.0.2.2:5000/api/") // emulator fallback; replace in local.properties for device testing
    // Read Google Maps API key from local.properties (not committed). Leave empty for CI or env injection.
    val googleMapsKey: String = (localProps.getProperty("google.maps.key") ?: "")

    defaultConfig {
        applicationId = "com.example.evcharger"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Expose API base URL to BuildConfig so app code can access it
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        // Inject the Google Maps API key into a string resource at build time so the key isn't checked in
        if (googleMapsKey.isNotBlank()) {
            resValue("string", "google_maps_key", "\"$googleMapsKey\"")
        } else {
            // Provide an empty value so builds don't fail when the key isn't present
            resValue("string", "google_maps_key", "\"\"")
        }
    }

    buildFeatures {
        viewBinding = true
        // enable Jetpack Compose
        compose = true
        // required to use buildConfigField
        buildConfig = true
    }

    // With Kotlin Compose plugin 2.1.x, the Compose compiler is bundled.
    // No explicit composeOptions.kotlinCompilerExtensionVersion needed.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Compose BOM + Material3 + Activity Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    // AndroidX + Material
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.fragment:fragment-ktx:1.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.activity:activity-ktx:1.7.2")
    // Retrofit + OkHttp + Gson
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.14")

    // ZXing (QR) - generation and embedded scanner
    implementation(libs.core)
    implementation(libs.zxing.android.embedded) // Scanner Activity

    // Google Maps & Location
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    // EncryptedSharedPreferences for secure token storage
    implementation("androidx.security:security-crypto:1.1.0")
}