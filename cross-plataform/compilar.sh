cd ~/Linka/cross-plataform

# 1. Limpa os arquivos compilados do C++ (.o e .so antigos)
make clean

# 2. Deleta a estrutura antiga gerada pelo androiddeployqt
rm -rf android-build
rm -rf armeabi-v7a
rm -f libLinka_armeabi-v7a.so

# 3. Regenera o Makefile do zero
~/Qt/5.15.2/android/bin/qmake linka.pro -spec android-clang ANDROID_ABIS="armeabi-v7a"

# 4. Recompila todo o código C++ com as alterações do QLabel
make -j$(nproc)

# 5. Prepara o ambiente e empacota o APK limpo
mkdir -p android-build/libs/armeabi-v7a
cp libLinka_armeabi-v7a.so android-build/libs/armeabi-v7a/

~/Qt/5.15.2/android/bin/androiddeployqt \
  --input android-Linka-deployment-settings.json \
  --output android-build \
  --android-platform android-30 \
  --jdk $JAVA_HOME \
  --gradle

# 6. Reinstala no dispositivo físico Samsung (com a chave -r para sobrescrever)
adb -s 42007f98e4524471 install -r android-build/build/outputs/apk/debug/android-build-debug.apk
