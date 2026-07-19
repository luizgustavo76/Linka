import java.io.File

tasks.register("gerarResolucoesIcons") {
    doLast {
        val pastaOriginal = file("${projectDir}/")
        val pastaRes = file("${projectDir}/src/main/res/drawable")

        if (pastaOriginal.exists() && pastaOriginal.isDirectory) {
            pastaOriginal.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".png")) {
                    val nome = file.name
                    println("⚙️ Redimensionando ícone com Java Process: $nome")
                    
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

                        // Usando ProcessBuilder do Java raiz: sem erros de escopo do Gradle!
                        ProcessBuilder("convert", file.absolutePath, "-resize", dimensao, arquivoDestino.absolutePath)
                            .inheritIO()
                            .start()
                            .waitFor()
                    }
                }
            }
        } else {
            println("⚠️ Pasta 'imagens_originais' não encontrada em: ${pastaOriginal.absolutePath}")
        }
    }
}

tasks.named("preBuild") {
    dependsOn("gerarResolucoesIcons")
}
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
