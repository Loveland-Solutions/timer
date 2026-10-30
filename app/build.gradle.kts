plugins {
    id("com.android.application")
}

android {
    namespace = "mn.lls.tiletimer"
    compileSdk = 36

    defaultConfig {
        applicationId = "mn.lls.tiletimer"
        minSdk = 33          // Galaxy S26 / Pixel 10a are far above this; 33 keeps the code simple
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Local test builds only, until Phase 2 adds the upload key.
            if (project.hasProperty("signWithDebugKey")) {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// No dependencies on purpose: plain framework APIs only, so the app stays tiny.
dependencies {
}
