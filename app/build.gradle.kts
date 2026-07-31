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

    signingConfigs {
        create("production") {
            val signingStorePath = System.getenv("SIGNING_STORE_FILE")
            if (signingStorePath != null) {
                storeFile = file(signingStorePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("production")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
