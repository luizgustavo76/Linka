import java.io.File

plugins {
    id("com.android.application")
}

// Task personalizada para redimensionar ícones via ImageMagick
tasks.register("gerarResolucoesIcons") {
    doLast {
        val pastaOriginal = file("${projectDir}")
        val pastaRes = file("${projectDir}/src/main/res")

        if (pastaOriginal.exists() && pastaOriginal.isDirectory) {
            pastaOriginal.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".png")) {
                    val nome = file.name
                    println("⚙️ Redimensionando ícone com ImageMagick: $nome")
                    
                    val tamanhos = mapOf(
                        "drawable-ldpi" to "24x24",
                        "drawable-mdpi" to "32x32",
                        "drawable-hdpi" to "48x48",
                        "drawable-xhdpi" to "64x64",
                        "drawable-xxhdpi" to "96x96"
                    )

                    tamanhos.forEach { (pasta, dimensao) ->
                        val destinoPasta = File(pastaRes, pasta)
                        if (!destinoPasta.exists()) destinoPasta.mkdirs()

                        val arquivoDestino = File(destinoPasta, nome)

                        try {
                            ProcessBuilder("convert", file.absolutePath, "-resize", dimensao, arquivoDestino.absolutePath)
                                .inheritIO()
                                .start()
                                .waitFor()
                        } catch (e: Exception) {
                            println("⚠️ Aviso: Falha ao executar 'convert'. Certifique-se de ter o ImageMagick instalado.")
                        }
                    }
                }
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn("gerarResolucoesIcons")
}

android {
    namespace = "com.LinkaProject.linkaLite"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.LinkaProject.linkaLite"
        minSdk = 3
        targetSdk = 9
        versionCode = 2
        versionName = "2.2"
    }

    androidResources {
        // Gerenciamento de recursos nativos via AAPT2
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = false
        }
        create("release") {
            storeFile = file("chave-linka.jks")
            storePassword = "Luiz08102012"
            keyAlias = "linka_key"
            keyPassword = "Luiz08102012"
            enableV1Signing = true
            enableV2Signing = false
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

// Dependências ficam FORA do bloco android { ... }
dependencies {
    implementation("com.android.support:support-v4:23.4.0")
}