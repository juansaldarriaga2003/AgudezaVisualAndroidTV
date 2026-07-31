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
        versionCode = 5
        versionName = "2.1.0"
    }

    flavorDimensions += "deployment"
    productFlavors {
        create("normal") {
            dimension = "deployment"
            versionNameSuffix = "-offline"
            buildConfigField("boolean", "KIOSK_MODE", "false")
        }
        create("kiosk") {
            dimension = "deployment"
            applicationIdSuffix = ".kiosk"
            versionNameSuffix = "-kiosk"
            buildConfigField("boolean", "KIOSK_MODE", "true")
        }
    }

    buildFeatures {
        buildConfig = true
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
