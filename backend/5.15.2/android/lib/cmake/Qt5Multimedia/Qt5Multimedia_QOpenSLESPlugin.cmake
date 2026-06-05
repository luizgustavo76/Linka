
add_library(Qt5::QOpenSLESPlugin MODULE IMPORTED)


_populate_Multimedia_plugin_properties(QOpenSLESPlugin RELEASE "audio/libplugins_audio_qtaudio_opensles_${ANDROID_ABI}.so" FALSE)

list(APPEND Qt5Multimedia_PLUGINS Qt5::QOpenSLESPlugin)
set_property(TARGET Qt5::Multimedia APPEND PROPERTY QT_ALL_PLUGINS_audio Qt5::QOpenSLESPlugin)
set_property(TARGET Qt5::QOpenSLESPlugin PROPERTY QT_PLUGIN_TYPE "audio")
set_property(TARGET Qt5::QOpenSLESPlugin PROPERTY QT_PLUGIN_EXTENDS "")
set_property(TARGET Qt5::QOpenSLESPlugin PROPERTY QT_PLUGIN_CLASS_NAME "QOpenSLESPlugin")
