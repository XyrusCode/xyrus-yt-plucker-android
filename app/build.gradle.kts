plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "xyrus.code.ytplucker"
    compileSdk = 35

    defaultConfig {
        applicationId = "xyrus.code.ytplucker"
        minSdk = 24
        targetSdk = 35
        versionCode = 13
        versionName = "4.2.1"

        val sentryDsn = (project.findProperty("sentryDsn") as String?)
            ?: System.getenv("SENTRY_DSN")
            ?: "https://c7912a9a54358f46a807910d06e3aa9c@o4505487881732096.ingest.us.sentry.io/4511751276462080"
        buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
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
        buildConfig = true
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

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.sentry.android)

    testImplementation(libs.junit)
}
