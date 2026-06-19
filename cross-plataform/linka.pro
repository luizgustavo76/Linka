QT += widgets network
CONFIG += c++
ANDROID_MIN_SDK_VERSION = 21
RESOURCES += resources.qrc
SOURCES += menu.cpp
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