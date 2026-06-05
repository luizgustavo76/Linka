
add_library(Qt5::QAndroidGamepadBackendPlugin MODULE IMPORTED)


_populate_Gamepad_plugin_properties(QAndroidGamepadBackendPlugin RELEASE "gamepads/libplugins_gamepads_androidgamepad_${ANDROID_ABI}.so" FALSE)

list(APPEND Qt5Gamepad_PLUGINS Qt5::QAndroidGamepadBackendPlugin)
set_property(TARGET Qt5::Gamepad APPEND PROPERTY QT_ALL_PLUGINS_gamepads Qt5::QAndroidGamepadBackendPlugin)
set_property(TARGET Qt5::QAndroidGamepadBackendPlugin PROPERTY QT_PLUGIN_TYPE "gamepads")
set_property(TARGET Qt5::QAndroidGamepadBackendPlugin PROPERTY QT_PLUGIN_EXTENDS "")
set_property(TARGET Qt5::QAndroidGamepadBackendPlugin PROPERTY QT_PLUGIN_CLASS_NAME "QAndroidGamepadBackendPlugin")
