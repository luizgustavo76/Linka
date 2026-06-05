
add_library(Qt5::GeoServiceProviderFactoryEsri MODULE IMPORTED)


_populate_Location_plugin_properties(GeoServiceProviderFactoryEsri RELEASE "geoservices/libplugins_geoservices_qtgeoservices_esri_${ANDROID_ABI}.so" FALSE)

list(APPEND Qt5Location_PLUGINS Qt5::GeoServiceProviderFactoryEsri)
set_property(TARGET Qt5::Location APPEND PROPERTY QT_ALL_PLUGINS_geoservices Qt5::GeoServiceProviderFactoryEsri)
set_property(TARGET Qt5::GeoServiceProviderFactoryEsri PROPERTY QT_PLUGIN_TYPE "geoservices")
set_property(TARGET Qt5::GeoServiceProviderFactoryEsri PROPERTY QT_PLUGIN_EXTENDS "")
set_property(TARGET Qt5::GeoServiceProviderFactoryEsri PROPERTY QT_PLUGIN_CLASS_NAME "GeoServiceProviderFactoryEsri")
