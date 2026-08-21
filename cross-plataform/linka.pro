QT += widgets network svg concurrent
!android {
    QT += openglwidgets
}

# Adiciona o módulo AndroidExtras apenas quando estiver compilando para Android
android {
    QT += androidextras
}

TEMPLATE = app
TARGET = Linka

# Fontes e Recursos do Projeto
SOURCES += menu.cpp
RESOURCES += resources.qrc

# As flags do Asyncify só serão aplicadas se você estiver compilando para a Web
contains(QT_ARCH, wasm) {
    QMAKE_LFLAGS += -sASYNCIFY -sASYNCIFY_STACK_SIZE=65536
}

# Configurações de Arquitetura e Plataforma
CONFIG += c++11
QMAKE_CXXFLAGS += -fpermissive
ANDROID_ABIS = armeabi-v7a 

# Configurações globais de Ícones
RC_ICONS = assets/icon.png
INCLUDEPATH += $$PWD/third_party

# Configurações globais de SDK fora de blocos (letras minúsculas para compatibilidade)
ANDROID_MIN_SDK_VERSION = 21
ANDROID_TARGET_SDK_VERSION = 30

# Bloco unificado de configurações específicas para Android (Ex: J2 Prime de testes)
android {
    # Força as variáveis dentro do escopo do Android
    REG_ANDROID_MIN_SDK_VERSION = 21
    REG_ANDROID_TARGET_SDK_VERSION = 30
    
    # Arquiteturas (apenas 32bits armeabi-v7a)
    ANDROID_ABIS = armeabi-v7a

    # Aponta para a sua pasta do manifesto
    ANDROID_PACKAGE_SOURCE_DIR = $$PWD/android

    # Garante que o Qt vai injetar as bibliotecas nativas de C++ dentro do APK
    ANDROID_EXTRA_LIBS = 
    
    # Diz ao compilador para embutir todas as dependências do Qt automaticamente
    CONFIG += android_install
}

# Arquivos de distribuição para o Qt Creator gerenciar
DISTFILES += \
    android/.gradle/9.2.0/checksums/checksums.lock \
    android/.gradle/9.2.0/fileChanges/last-build.bin \
    android/.gradle/9.2.0/fileHashes/fileHashes.bin \
    android/.gradle/9.2.0/fileHashes/fileHashes.lock \
    android/.gradle/9.2.0/gc.properties \
    android/.gradle/buildOutputCleanup/buildOutputCleanup.lock \
    android/.gradle/buildOutputCleanup/cache.properties \
    android/.gradle/vcs-1/gc.properties \
    android/AndroidManifest.xml \
    android/build.gradle \
    android/gradle.properties \
    android/gradle/wrapper/gradle-wrapper.jar \
    android/gradle/wrapper/gradle-wrapper.properties \
    android/gradlew \
    android/gradlew.bat \
    android/res/values/libs.xml