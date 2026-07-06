plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "xyrus.code.ytplucker"
    compileSdk = 35

    defaultConfig {
        applicationId = "xyrus.code.ytplucker"
        minSdk = 24
        targetSdk = 35
        versionCode = 7
        versionName = "3.1.1"
    }

    // One shared, committed signing key (identity: xyrus.code.yt-plucker) used by every build so
    // updates never conflict — a device installs a new version straight over the old one. The key
    // is intentionally public: fine for a personally self-distributed GitHub app, not Play-grade.
    signingConfigs {
        create("shared") {
            storeFile = rootProject.file("keystore/ytplucker.jks")
            storePassword = "ytplucker"
            keyAlias = "ytplucker"
            keyPassword = "ytplucker"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            signingConfig = signingConfigs.getByName("shared")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    // Split by ABI so a device installs only its native slice. youtubedl-android
    // bundles a Python runtime + yt-dlp + ffmpeg per ABI, so a universal APK is large.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true // also emit a universal APK for convenience
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
        compose = true
    }

    // Don't let a non-critical lint rule fail the release build in CI.
    lint {
        abortOnError = false
    }

    // youtubedl-android unpacks its Python/yt-dlp payload from the APK at runtime,
    // so native libs must be extractable and packaged the legacy way.
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)

    implementation(libs.youtubedl.android.library)
    implementation(libs.youtubedl.android.ffmpeg)
}
