
add_library(Qt5::QTextToSpeechEngineAndroid MODULE IMPORTED)


_populate_TextToSpeech_plugin_properties(QTextToSpeechEngineAndroid RELEASE "texttospeech/libplugins_texttospeech_qttexttospeech_android_${ANDROID_ABI}.so" FALSE)

list(APPEND Qt5TextToSpeech_PLUGINS Qt5::QTextToSpeechEngineAndroid)
set_property(TARGET Qt5::TextToSpeech APPEND PROPERTY QT_ALL_PLUGINS_texttospeech Qt5::QTextToSpeechEngineAndroid)
set_property(TARGET Qt5::QTextToSpeechEngineAndroid PROPERTY QT_PLUGIN_TYPE "texttospeech")
set_property(TARGET Qt5::QTextToSpeechEngineAndroid PROPERTY QT_PLUGIN_EXTENDS "")
set_property(TARGET Qt5::QTextToSpeechEngineAndroid PROPERTY QT_PLUGIN_CLASS_NAME "QTextToSpeechEngineAndroid")
