
add_library(Qt5::QGeoPositionInfoSourceFactoryAndroid MODULE IMPORTED)


_populate_Positioning_plugin_properties(QGeoPositionInfoSourceFactoryAndroid RELEASE "position/libplugins_position_qtposition_android_${ANDROID_ABI}.so" FALSE)

list(APPEND Qt5Positioning_PLUGINS Qt5::QGeoPositionInfoSourceFactoryAndroid)
set_property(TARGET Qt5::Positioning APPEND PROPERTY QT_ALL_PLUGINS_position Qt5::QGeoPositionInfoSourceFactoryAndroid)
set_property(TARGET Qt5::QGeoPositionInfoSourceFactoryAndroid PROPERTY QT_PLUGIN_TYPE "position")
set_property(TARGET Qt5::QGeoPositionInfoSourceFactoryAndroid PROPERTY QT_PLUGIN_EXTENDS "")
set_property(TARGET Qt5::QGeoPositionInfoSourceFactoryAndroid PROPERTY QT_PLUGIN_CLASS_NAME "QGeoPositionInfoSourceFactoryAndroid")
