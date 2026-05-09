QT += widgets network
CONFIG += c++17
RESOURCES += resources.qrc
SOURCES += menu.cpp
QT -= ssl
INCLUDEPATH += $$PWD/third_party
RC_ICONS = assets/icon.ico