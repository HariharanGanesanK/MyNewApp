plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Required for Kotlin 2.0+ (Compose Compiler)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

android {
    namespace = "com.example.helloworldapp"
    compileSdk = 36   // Android 15

    defaultConfig {
        applicationId = "com.example.helloworldapp"
        minSdk = 24
        targetSdk = 36
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
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += "-Xcontext-receivers"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // Kotlin 2.x requires Compose Compiler 1.6.11
        kotlinCompilerExtensionVersion = "1.6.11"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {

    // ---------------------------------------------------
    // ANDROIDX CORE
    // ---------------------------------------------------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // ---------------------------------------------------
    // JETPACK COMPOSE
    // ---------------------------------------------------
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation("androidx.compose.ui:ui-text:1.7.5")
    implementation("androidx.compose.foundation:foundation:1.7.5")
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.animation:animation:1.7.5")

    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    // Compose Add-ons
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0")
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // ---------------------------------------------------
    // NETWORKING (Retrofit + OkHttp)
    // ---------------------------------------------------
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // ---------------------------------------------------
    // COROUTINES
    // ---------------------------------------------------
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ---------------------------------------------------
    // IMAGE LOADING (COIL)
    // ---------------------------------------------------
    implementation("io.coil-kt:coil-compose:2.2.2")

    // ---------------------------------------------------
    // BIOMETRICS
    // ---------------------------------------------------
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // ---------------------------------------------------
    // SECURE STORAGE
    // ---------------------------------------------------
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ---------------------------------------------------
    // MQTT SUPPORT (Paho MQTT)
    // ---------------------------------------------------
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    // REQUIRED FIX FOR Paho MQTT Crash:
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.0.0")
    // ---------------------------------------------------

    // ---------------------------------------------------
    // TESTING
    // ---------------------------------------------------
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
