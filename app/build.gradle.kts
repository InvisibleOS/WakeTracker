plugins {
    alias(libs.plugins.android.application)
    // kotlin.android is COMPLETELY GONE
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.shivansh.waketracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shivansh.waketracker"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            signingConfig = signingConfigs.getByName("debug")

            if (System.getenv("MYAPP_RELEASE_STORE_FILE") != null) {
                signingConfigs.create("release") {
                    storeFile = file(System.getenv("MYAPP_RELEASE_STORE_FILE"))
                    storePassword = System.getenv("MYAPP_RELEASE_STORE_PASSWORD")
                    keyAlias = System.getenv("MYAPP_RELEASE_KEY_ALIAS")
                    keyPassword = System.getenv("MYAPP_RELEASE_KEY_PASSWORD")
                }
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
    // kotlinOptions block is COMPLETELY GONE
}

// THIS is the modern replacement for kotlinOptions
kotlin {
    jvmToolchain(17)
}

dependencies {
    // --- UI LIBRARIES ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.12.0")

    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.graphics:graphics-shapes:1.1.0")

    // --- DATABASE & BACKGROUND ---
    val roomVersion = "2.8.0"
    val workVersion = "2.11.2"

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("androidx.work:work-runtime-ktx:$workVersion")
}