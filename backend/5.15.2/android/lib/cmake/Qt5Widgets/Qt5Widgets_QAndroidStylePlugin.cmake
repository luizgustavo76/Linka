
add_library(Qt5::QAndroidStylePlugin MODULE IMPORTED)


_populate_Widgets_plugin_properties(QAndroidStylePlugin RELEASE "styles/libplugins_styles_qandroidstyle_${ANDROID_ABI}.so" FALSE)

list(APPEND Qt5Widgets_PLUGINS Qt5::QAndroidStylePlugin)
set_property(TARGET Qt5::Widgets APPEND PROPERTY QT_ALL_PLUGINS_styles Qt5::QAndroidStylePlugin)
set_property(TARGET Qt5::QAndroidStylePlugin PROPERTY QT_PLUGIN_TYPE "styles")
set_property(TARGET Qt5::QAndroidStylePlugin PROPERTY QT_PLUGIN_EXTENDS "")
set_property(TARGET Qt5::QAndroidStylePlugin PROPERTY QT_PLUGIN_CLASS_NAME "QAndroidStylePlugin")
