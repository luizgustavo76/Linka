
add_library(Qt5::QAndroidSGVideoNodeFactoryPlugin MODULE IMPORTED)


_populate_Multimedia_plugin_properties(QAndroidSGVideoNodeFactoryPlugin RELEASE "video/videonode/libplugins_video_videonode_qtsgvideonode_android_${ANDROID_ABI}.so" FALSE)

list(APPEND Qt5Multimedia_PLUGINS Qt5::QAndroidSGVideoNodeFactoryPlugin)
set_property(TARGET Qt5::Multimedia APPEND PROPERTY QT_ALL_PLUGINS_video_videonode Qt5::QAndroidSGVideoNodeFactoryPlugin)
set_property(TARGET Qt5::QAndroidSGVideoNodeFactoryPlugin PROPERTY QT_PLUGIN_TYPE "video/videonode")
set_property(TARGET Qt5::QAndroidSGVideoNodeFactoryPlugin PROPERTY QT_PLUGIN_EXTENDS "Qt::Quick")
set_property(TARGET Qt5::QAndroidSGVideoNodeFactoryPlugin PROPERTY QT_PLUGIN_CLASS_NAME "QAndroidSGVideoNodeFactoryPlugin")
