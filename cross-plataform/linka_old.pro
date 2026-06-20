TEMPLATE = lib
QT += core gui network declarative script
CONFIG += plugin
# Removemos o c++11 puro porque o GCC 4.4.3 do Necessitas não aguenta tudo dele. 
# Usaremos flags tolerantes.

TARGET = linka_old

# Módulos base Corrigidos para o padrão do Qt 4.8.2
QT += core gui network declarative
# 'gui' no Qt4 já inclui os Widgets. 'declarative' substitui qml e quick.

QT -= ssl

# Arquivos do projeto
RESOURCES += resources.qrc
SOURCES += menuQt48.cpp
INCLUDEPATH += $$PWD/third_party
QMAKE_CXXFLAGS += -fpermissive