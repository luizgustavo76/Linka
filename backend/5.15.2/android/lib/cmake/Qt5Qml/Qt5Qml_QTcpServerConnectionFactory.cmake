
add_library(Qt5::QTcpServerConnectionFactory MODULE IMPORTED)


_populate_Qml_plugin_properties(QTcpServerConnectionFactory RELEASE "qmltooling/libplugins_qmltooling_qmldbg_tcp_${ANDROID_ABI}.so" FALSE)

list(APPEND Qt5Qml_PLUGINS Qt5::QTcpServerConnectionFactory)
set_property(TARGET Qt5::Qml APPEND PROPERTY QT_ALL_PLUGINS_qmltooling Qt5::QTcpServerConnectionFactory)
set_property(TARGET Qt5::QTcpServerConnectionFactory PROPERTY QT_PLUGIN_TYPE "qmltooling")
set_property(TARGET Qt5::QTcpServerConnectionFactory PROPERTY QT_PLUGIN_EXTENDS "")
set_property(TARGET Qt5::QTcpServerConnectionFactory PROPERTY QT_PLUGIN_CLASS_NAME "QTcpServerConnectionFactory")
