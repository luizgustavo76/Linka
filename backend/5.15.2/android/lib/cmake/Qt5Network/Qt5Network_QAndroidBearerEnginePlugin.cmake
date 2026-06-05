
add_library(Qt5::QAndroidBearerEnginePlugin MODULE IMPORTED)


_populate_Network_plugin_properties(QAndroidBearerEnginePlugin RELEASE "bearer/libplugins_bearer_qandroidbearer_${ANDROID_ABI}.so" FALSE)

list(APPEND Qt5Network_PLUGINS Qt5::QAndroidBearerEnginePlugin)
set_property(TARGET Qt5::Network APPEND PROPERTY QT_ALL_PLUGINS_bearer Qt5::QAndroidBearerEnginePlugin)
set_property(TARGET Qt5::QAndroidBearerEnginePlugin PROPERTY QT_PLUGIN_TYPE "bearer")
set_property(TARGET Qt5::QAndroidBearerEnginePlugin PROPERTY QT_PLUGIN_EXTENDS "")
set_property(TARGET Qt5::QAndroidBearerEnginePlugin PROPERTY QT_PLUGIN_CLASS_NAME "QAndroidBearerEnginePlugin")
