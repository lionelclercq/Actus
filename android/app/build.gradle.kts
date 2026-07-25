plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "fr.actus.sync"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.actus.sync"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.1.2"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("ACTUS_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ACTUS_KEYSTORE_PASSWORD").orEmpty()
                keyAlias = System.getenv("ACTUS_KEY_ALIAS") ?: "actus"
                keyPassword = System.getenv("ACTUS_KEY_PASSWORD").orEmpty()
            } else {
                // Même clé que l'APK v1.1.0 (debug) pour permettre la mise à jour sans désinstallation.
                storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                storePassword = "android"
                keyAlias = "AndroidDebugKey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

@Suppress("DEPRECATION")
android.applicationVariants.configureEach {
    outputs.configureEach {
        (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
            "actus-sync-${buildType.name}.apk"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
