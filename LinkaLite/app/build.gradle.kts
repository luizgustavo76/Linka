plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.linkalite"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.LinkaProject.linkalite"
        minSdk = 9
        targetSdk = 9
        versionCode = 1
        versionName = "1.0"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
}
