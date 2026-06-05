
add_library(Qt5::QAndroidMediaServicePlugin MODULE IMPORTED)


_populate_Multimedia_plugin_properties(QAndroidMediaServicePlugin RELEASE "mediaservice/libplugins_mediaservice_qtmedia_android_${ANDROID_ABI}.so" FALSE)

list(APPEND Qt5Multimedia_PLUGINS Qt5::QAndroidMediaServicePlugin)
set_property(TARGET Qt5::Multimedia APPEND PROPERTY QT_ALL_PLUGINS_mediaservice Qt5::QAndroidMediaServicePlugin)
set_property(TARGET Qt5::QAndroidMediaServicePlugin PROPERTY QT_PLUGIN_TYPE "mediaservice")
set_property(TARGET Qt5::QAndroidMediaServicePlugin PROPERTY QT_PLUGIN_EXTENDS "")
set_property(TARGET Qt5::QAndroidMediaServicePlugin PROPERTY QT_PLUGIN_CLASS_NAME "QAndroidMediaServicePlugin")
