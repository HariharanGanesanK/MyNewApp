plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // ✅ Required for Kotlin 2.0+ (Compose Compiler plugin)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

android {
    namespace = "com.example.helloworldapp"

    // ✅ Targeting the latest Android SDK
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.helloworldapp"
        minSdk = 24  // ✅ Android 7.0 (Nougat) and above
        targetSdk = 36 // ✅ Android 15
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Enables detailed debug builds
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        // ✅ Enables advanced Compose and Kotlin 2.0 features
        freeCompilerArgs += "-Xcontext-receivers"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // ✅ Must match Kotlin 2.0.x (use 1.6.11 or higher)
        kotlinCompilerExtensionVersion = "1.6.11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // --- Core AndroidX ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // --- Jetpack Compose ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation("androidx.compose.ui:ui-text:1.7.5") // ✅ Required for KeyboardOptions & ImeAction
    implementation("androidx.compose.foundation:foundation:1.7.5") // Focus & Keyboard control
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // ✅ Material Icons (Visibility/VisibilityOff icons)
    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    // --- Networking (OkHttp + Retrofit) ---
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // --- Image Loading (Coil for Compose) ---
    implementation("io.coil-kt:coil-compose:2.2.2")

    // --- ✅ Biometric Authentication ---
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // --- ✅ Secure Storage (EncryptedSharedPreferences) ---
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // --- ✅ Optional: Animation support for Compose ---
    implementation("androidx.compose.animation:animation:1.7.5")

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
