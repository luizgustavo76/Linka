
add_library(Qt5::QAndroidIntegrationPlugin MODULE IMPORTED)


_populate_Gui_plugin_properties(QAndroidIntegrationPlugin RELEASE "platforms/libplugins_platforms_qtforandroid_${ANDROID_ABI}.so" FALSE)

list(APPEND Qt5Gui_PLUGINS Qt5::QAndroidIntegrationPlugin)
set_property(TARGET Qt5::Gui APPEND PROPERTY QT_ALL_PLUGINS_platforms Qt5::QAndroidIntegrationPlugin)
set_property(TARGET Qt5::QAndroidIntegrationPlugin PROPERTY QT_PLUGIN_TYPE "platforms")
set_property(TARGET Qt5::QAndroidIntegrationPlugin PROPERTY QT_PLUGIN_EXTENDS "")
set_property(TARGET Qt5::QAndroidIntegrationPlugin PROPERTY QT_PLUGIN_CLASS_NAME "QAndroidIntegrationPlugin")
