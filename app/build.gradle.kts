import java.util.Properties

plugins {
    id("com.android.application")
}

val keystoreFile = rootProject.file("keystore.properties")

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

    signingConfigs {
        create("release") {
            // Filled in from keystore.properties; see the check at the end of this file.
            if (keystoreFile.exists()) {
                val keystore = Properties().apply { keystoreFile.inputStream().use { load(it) } }
                storeFile = rootProject.file(keystore.getProperty("storeFile"))
                storePassword = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("release")
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

// A release build without the upload key must fail clearly, not produce an unsigned file.
gradle.taskGraph.whenReady {
    val wantsRelease = allTasks.any { it.name in setOf("bundleRelease", "assembleRelease") }
    if (wantsRelease && !keystoreFile.exists()) {
        throw GradleException(
            "Release builds need keystore.properties and keystore/upload.jks. See play/README.md.")
    }
}
