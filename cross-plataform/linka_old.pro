TEMPLATE = lib
CONFIG += plugin c++11
TARGET = linka_old

# Módulos base
QT += core gui widgets network qml quick
QT -= ssl

# Arquivos do projeto
RESOURCES += resources.qrc
SOURCES += menuQt48.cpp
INCLUDEPATH += $$PWD/third_party
QMAKE_CXXFLAGS += -fpermissive

# Configurações estritas para Android
android {
    message("--- LIMPANDO HERANÇA DE PC E AJUSTANDO ANDROID ---")

    # 1. Remove absolutamente qualquer menção às pastas de PC que o qmake injeta por padrão
    INCLUDEPATH -= /home/luiz/Qt5.1.0/5.1.0/gcc_64/include
    INCLUDEPATH -= /home/luiz/Qt5.1.0/5.1.0/gcc_64/include/QtQuick
    INCLUDEPATH -= /home/luiz/Qt5.1.0/5.1.0/gcc_64/include/QtQml
    INCLUDEPATH -= /home/luiz/Qt5.1.0/5.1.0/gcc_64/include/QtWidgets
    INCLUDEPATH -= /home/luiz/Qt5.1.0/5.1.0/gcc_64/include/QtNetwork
    INCLUDEPATH -= /home/luiz/Qt5.1.0/5.1.0/gcc_64/include/QtGui
    INCLUDEPATH -= /home/luiz/Qt5.1.0/5.1.0/gcc_64/include/QtCore

    LIBS -= -L/home/luiz/Qt5.1.0/5.1.0/gcc_64/lib

    # 2. Garante as flags corretas para arquitetura ARM do Android
    QMAKE_CFLAGS += -fPIC
    QMAKE_CXXFLAGS += -fPIC
    DEFINES += _GNU_SOURCE

    # 3. Adiciona os includes do Android E do C++ STL (Resolve o erro do <algorithm>)
    # É vital incluir a pasta 'gnu-libstdc++/4.8/include' para o compilador achar os headers de C++
    INCLUDEPATH += \
        /home/luiz/Qt5.1.0/5.1.0/android_armv7/include \
        /home/luiz/Downloads/android-ndk-r9d/platforms/android-14/arch-arm/usr/include \
        /home/luiz/Downloads/android-ndk-r9d/sources/cxx-stl/gnu-libstdc++/4.8/include \
        /home/luiz/Downloads/android-ndk-r9d/sources/cxx-stl/gnu-libstdc++/4.8/libs/armeabi-v7a/include

    # 4. Links de bibliotecas corretos do Android ARMv7
    LIBS += \
        -L/home/luiz/Qt5.1.0/5.1.0/android_armv7/lib \
        -L/home/luiz/Downloads/android-ndk-r9d/platforms/android-14/arch-arm/usr/lib \
        -L/home/luiz/Downloads/android-ndk-r9d/sources/cxx-stl/gnu-libstdc++/4.8/libs/armeabi-v7a \
        -lgnustl_shared -lsupc++ -llog -lz -lm -ldl -lc -lgcc

    # Metadados do deploy do Android
    ANDROID_ABIS = armeabi-v7a
    ANDROID_MIN_SDK_VERSION = 14
    ANDROID_TARGET_SDK_VERSION = 14
    ANDROID_COMPILE_SDK_VERSION = 14
    ANDROID_PACKAGE_SOURCE_DIR = $$PWD/android
}

# Arquivos extras de deploy
DISTFILES += \
    android/AndroidManifest.xml \
    android/build.gradle \
    android/res/values/libs.xml

OTHER_FILES += \
    android/src/org/kde/necessitas/ministro/IMinistroCallback.aidl \
    android/src/org/kde/necessitas/ministro/IMinistro.aidl \
    android/src/org/qtproject/qt5/android/bindings/QtService.java \
    android/src/org/qtproject/qt5/android/bindings/QtActivity.java \
    android/src/org/qtproject/qt5/android/bindings/QtApplication.java \
    android/src/org/qtproject/qt5/android/bindings/QtActivityLoader.java \
    android/src/org/qtproject/qt5/android/bindings/QtServiceLoader.java \
    android/src/org/qtproject/qt5/android/bindings/QtLoader.java \
    android/res/layout/splash.xml \
    android/res/values/strings.xml
