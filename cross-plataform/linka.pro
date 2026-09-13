QT += widgets network concurrent
greaterThan(QT_MAJOR_VERSION, 5) {
    QT += openglwidgets
} else {
    QT += opengl
}

# Adiciona o módulo AndroidExtras apenas quando estiver compilando para Android
android {
    QT += androidextras
}

TEMPLATE = app
TARGET = Linka

# Configurações de Arquitetura e Plataforma
CONFIG += c++11 static
QMAKE_CXXFLAGS += -fpermissive

# Organiza os arquivos objeto (.o), MOCs e UIs para evitar recompilação contínua de tudo
OBJECTS_DIR = $$PWD/build/objs
MOC_DIR     = $$PWD/build/mocs
UI_DIR      = $$PWD/build/uis

# Linka estaticamente o OpenSSL para evitar erros de HTTPS/SSL em tempo de execução
win32 {
    LIBS += -lssl -lcrypto -lws2_32 -lcrypt32
} else:unix:!android {
    LIBS += -lssl -lcrypto
}

# Fontes e Recursos do Projeto
SOURCES += menu.cpp
RESOURCES += resources.qrc

# As flags do Asyncify só serão aplicadas se você estiver compilando para a Web
contains(QT_ARCH, wasm) {
    QMAKE_LFLAGS += -sASYNCIFY -sASYNCIFY_STACK_SIZE=65536
}

# Configurações globais de Ícones
RC_ICONS = assets/icon.png
INCLUDEPATH += $$PWD/third_party

# Configurações globais de SDK fora de blocos
ANDROID_MIN_SDK_VERSION = 21
ANDROID_TARGET_SDK_VERSION = 30

# Bloco unificado de configurações específicas para Android
android {
    REG_ANDROID_MIN_SDK_VERSION = 21
    REG_ANDROID_TARGET_SDK_VERSION = 30
    ANDROID_ABIS = armeabi-v7a
    ANDROID_PACKAGE_SOURCE_DIR = $$PWD/android
    ANDROID_EXTRA_LIBS = 
    CONFIG += android_install
}

# Arquivos de distribuição para o Qt Creator gerenciar
DISTFILES += \
    android/AndroidManifest.xml \
    android/build.gradle \
    android/gradle.properties \
    android/gradlew \
    android/gradlew.bat