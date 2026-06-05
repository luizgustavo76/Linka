
add_library(Qt5::QQmlInspectorServiceFactory MODULE IMPORTED)


_populate_Qml_plugin_properties(QQmlInspectorServiceFactory RELEASE "qmltooling/libplugins_qmltooling_qmldbg_inspector_${ANDROID_ABI}.so" FALSE)

list(APPEND Qt5Qml_PLUGINS Qt5::QQmlInspectorServiceFactory)
set_property(TARGET Qt5::Qml APPEND PROPERTY QT_ALL_PLUGINS_qmltooling Qt5::QQmlInspectorServiceFactory)
set_property(TARGET Qt5::QQmlInspectorServiceFactory PROPERTY QT_PLUGIN_TYPE "qmltooling")
set_property(TARGET Qt5::QQmlInspectorServiceFactory PROPERTY QT_PLUGIN_EXTENDS "")
set_property(TARGET Qt5::QQmlInspectorServiceFactory PROPERTY QT_PLUGIN_CLASS_NAME "QQmlInspectorServiceFactory")
