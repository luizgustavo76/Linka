QT += widgets network
CONFIG += c++
ANDROID_MIN_SDK_VERSION = 21
RESOURCES += resources.qrc
SOURCES += menuQt48.cpp
QT -= ssl
INCLUDEPATH += $$PWD/third_party
RC_ICONS = assets/icon.ico
ANDROID_ABIS = armeabi-v7a arm64-v8a
ANDROID_MIN_SDK_VERSION = 21
ANDROID_TARGET_SDK_VERSION = 21
ANDROID_COMPILE_SDK_VERSION = 21
QMAKE_CXXFLAGS += -fpermissive
CONFIG += c++11
DISTFILES += \
    android/AndroidManifest.xml \
    android/build.gradle \
    android/res/values/libs.xml

ANDROID_PACKAGE_SOURCE_DIR = $$PWD/android

OTHER_FILES += \
    android/src/org/kde/necessitas/ministro/IMinistroCallback.aidl \
    android/src/org/kde/necessitas/ministro/IMinistro.aidl \
    android/src/org/qtproject/qt5/android/bindings/QtService.java \
    android/src/org/qtproject/qt5/android/bindings/QtActivity.java \
    android/src/org/qtproject/qt5/android/bindings/QtApplication.java \
    android/src/org/qtproject/qt5/android/bindings/QtActivityLoader.java \
    android/src/org/qtproject/qt5/android/bindings/QtServiceLoader.java \
    android/src/org/qtproject/qt5/android/bindings/QtLoader.java \
    android/res/values-it/strings.xml \
    android/res/values-nb/strings.xml \
    android/res/values-in/strings.xml \
    android/res/values-pl/strings.xml \
    android/res/layout/splash.xml \
    android/res/values-ms/strings.xml \
    android/res/values-ru/strings.xml \
    android/res/values-de/strings.xml \
    android/res/values-el/strings.xml \
    android/res/values-et/strings.xml \
    android/res/values-se/strings.xml \
    android/res/values-zh-rCN/strings.xml \
    android/res/values-ro/strings.xml \
    android/res/values-nl/strings.xml \
    android/res/values-fa/strings.xml \
    android/res/values/strings.xml \
    android/res/values-zh-rTW/strings.xml \
    android/res/values-fr/strings.xml \
    android/res/values-pt-rBR/strings.xml \
    android/res/values-es/strings.xml \
    android/res/values-ja/strings.xml