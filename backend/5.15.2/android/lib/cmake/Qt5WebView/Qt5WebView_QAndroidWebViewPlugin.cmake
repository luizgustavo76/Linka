
add_library(Qt5::QAndroidWebViewPlugin MODULE IMPORTED)


_populate_WebView_plugin_properties(QAndroidWebViewPlugin RELEASE "webview/libplugins_webview_qtwebview_android_${ANDROID_ABI}.so" FALSE)

list(APPEND Qt5WebView_PLUGINS Qt5::QAndroidWebViewPlugin)
set_property(TARGET Qt5::WebView APPEND PROPERTY QT_ALL_PLUGINS_webview Qt5::QAndroidWebViewPlugin)
set_property(TARGET Qt5::QAndroidWebViewPlugin PROPERTY QT_PLUGIN_TYPE "webview")
set_property(TARGET Qt5::QAndroidWebViewPlugin PROPERTY QT_PLUGIN_EXTENDS "")
set_property(TARGET Qt5::QAndroidWebViewPlugin PROPERTY QT_PLUGIN_CLASS_NAME "QAndroidWebViewPlugin")
