
add_library(Qt5::QCounterGesturePlugin MODULE IMPORTED)


_populate_Sensors_plugin_properties(QCounterGesturePlugin RELEASE "sensors/libplugins_sensors_qtsensors_android_${ANDROID_ABI}.so" FALSE)

list(APPEND Qt5Sensors_PLUGINS Qt5::QCounterGesturePlugin)
set_property(TARGET Qt5::Sensors APPEND PROPERTY QT_ALL_PLUGINS_sensors Qt5::QCounterGesturePlugin)
set_property(TARGET Qt5::QCounterGesturePlugin PROPERTY QT_PLUGIN_TYPE "sensors")
set_property(TARGET Qt5::QCounterGesturePlugin PROPERTY QT_PLUGIN_EXTENDS "")
set_property(TARGET Qt5::QCounterGesturePlugin PROPERTY QT_PLUGIN_CLASS_NAME "QCounterGesturePlugin")
