QT += widgets network
CONFIG += c++
ANDROID_MIN_SDK_VERSION = 21
RESOURCES += resources.qrc
SOURCES += menu.cpp
QT -= ssl
INCLUDEPATH += $$PWD/third_party
RC_ICONS = assets/icon.ico