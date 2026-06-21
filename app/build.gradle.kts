plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.tatliving.cosmohid"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.tatliving.cosmohid"
        minSdk = 28          // Android 9 — BluetoothHidDevice API; matches the Cosmo Communicator
        targetSdk = 28       // target the Cosmo's own API level to avoid picky-installer rejections
        versionCode = 1
        versionName = "0.1-v1"
    }

    signingConfigs {
        // Force v1 (JAR) signing in addition to v2. AGP disables v1 for debug when
        // minSdk >= 24, but many Android 9 sideload installers still require it, and
        // its absence shows up as a vague "internal error occurred" on the device.
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
