EXTRA_INCLUDEPATH += /home/qt/openssl/android/openssl-1.1.1g/include
host_build {
    QT_CPU_FEATURES.x86_64 = mmx sse sse2
} else {
    QT_CPU_FEATURES.arm64 = neon
}
QT.global_private.enabled_features = alloca_h alloca android-style-assets dlopen gui network posix_fallocate reduce_exports relocatable sql system-zlib testlib widgets xml
QT.global_private.disabled_features = sse2 alloca_malloc_h avx2 private_tests dbus dbus-linked gc_binaries intelcet libudev reduce_relocations release_tools stack-protector-strong zstd
QMAKE_LIBS_LIBDL = 
QT_COORD_TYPE = double
QMAKE_LIBS_ZLIB = -lz
CONFIG += cross_compile compile_examples enable_new_dtags neon precompile_header
QT_BUILD_PARTS += libs
