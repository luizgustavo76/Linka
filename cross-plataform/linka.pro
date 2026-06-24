QT += widgets network
TEMPLATE = app
ANDROID_MIN_SDK_VERSION = 21
RESOURCES += resources.qrc
SOURCES += menu.cpp
QT -= ssl
# Remove de todas as variáveis possíveis de plugins
QTPLUGIN.bearer -= qandroidbearer
QTPLUGIN -= qandroidbearer

# Força o compilador a não ligar o plugin de monitorização de redes do Android
DEFINES += QT_NO_BEARERMANAGEMENT
INCLUDEPATH += $$PWD/third_party
RC_ICONS = assets/icon.ico
ANDROID_ABIS = armeabi-v7a arm64-v8a
ANDROID_MIN_SDK_VERSION = 21
ANDROID_TARGET_SDK_VERSION = 30
ANDROID_COMPILE_SDK_VERSION = 30
QMAKE_CXXFLAGS += -fpermissive
CONFIG += c++11
DISTFILES += \
    android/AndroidManifest.xml \
    android/AndroidManifest.xml \
    android/build.gradle \
    android/build.gradle \
    android/gradle.properties \
    android/gradle/wrapper/gradle-wrapper.jar \
    android/gradle/wrapper/gradle-wrapper.properties \
    android/gradlew \
    android/gradlew.bat \
    android/res/values/libs.xml \
    android/res/values/libs.xml

ANDROID_PACKAGE_SOURCE_DIR = $$PWD/android