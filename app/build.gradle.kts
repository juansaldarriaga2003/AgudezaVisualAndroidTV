plugins {
    id("com.android.application")
}

android {
    namespace = "com.agudezavisual.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.agudezavisual.tv"
        minSdk = 21
        targetSdk = 35
        versionCode = 4
        versionName = "2.0.0-offline"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
