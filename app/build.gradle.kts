plugins {
    id("com.android.application")
}

android {
    namespace = "com.loveland.tiletimer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.loveland.tiletimer"
        minSdk = 33          // Galaxy S26 / Pixel 10a are far above this; 33 keeps the code simple
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

// No dependencies on purpose: plain framework APIs only, so the app is tiny
// and can also be built without Gradle (see build-noide.sh).
dependencies {
}
