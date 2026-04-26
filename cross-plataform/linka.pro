QT += widgets network
CONFIG += c++17

RESOURCES += resources.qrc
SOURCES += main.cpp\
    menu.cpp\
    login.cpp\
    config.cpp
HEADERS += global.h\
    config.h
QT -= ssl
INCLUDEPATH += $$PWD/third_party