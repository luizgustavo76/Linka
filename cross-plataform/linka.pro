QT += widgets network

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
RC_ICONS = assets/icon.ico
INCLUDEPATH += $$PWD/third_party
# Certifique-se de que essas linhas estão fora de qualquer bloco e escritas com letras MINÚSCULAS (o Qt novo é fresco com isso):
ANDROID_MIN_SDK_VERSION = 21
ANDROID_TARGET_SDK_VERSION = 30

# Agora, garanta o bloco correto do 'android':
android {
    # Força as variáveis dentro do escopo do Android
    REG_ANDROID_MIN_SDK_VERSION = 21
    REG_ANDROID_TARGET_SDK_VERSION = 30
    
    # Arquiteturas (apenas armeabi-v7a para testar no J2 Prime sem misturar 64bits)
    ANDROID_ABIS = armeabi-v7a

    # Aponta para a sua pasta do manifesto
    ANDROID_PACKAGE_SOURCE_DIR = $$PWD/android
}
android {
    # Garante que o Qt vai injetar as bibliotecas nativas de C++ dentro do APK
    ANDROID_EXTRA_LIBS = 
    
    # Diz ao compilador para embutir todas as dependências do Qt automaticamente
    CONFIG += android_install
}

# Arquivos de distribuição para o Qt Creator gerenciar
DISTFILES += \
    android/AndroidManifest.xml \
    android/build.gradle \
    android/gradle.properties \
    android/gradle/wrapper/gradle-wrapper.jar \
    android/gradle/wrapper/gradle-wrapper.properties \
    android/gradlew \
    android/gradlew.bat \
    android/res/values/libs.xml