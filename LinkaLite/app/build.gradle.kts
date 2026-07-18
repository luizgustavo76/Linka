plugins {
    id("com.android.application")
}

android {
    // CORREÇÃO 1: Mudado para 'linkaLite' com L maiúsculo para bater com seu Java
    namespace = "com.LinkaProject.linkaLite"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.LinkaProject.linkaLite"

        // Mantido em 9 (Android 2.3), o limite máximo de retrocompatibilidade do Gradle moderno
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
    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = false
        }
    }
    
    // CORREÇÃO 2: Removido o conflito do Java 8. 
    // Deixando o Java 17, o JDK 21 compila feliz e sem avisos obsoletos.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Se precisar de alguma biblioteca antiga de JSON, ela entraria aqui.
    // Como estamos usando o org.json nativo do Android, pode deixar vazio por enquanto.
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}