#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"

mode=${1:-apk}
if [[ $mode != apk && $mode != desktop ]]; then
    echo "Usage: $0 [apk|desktop]" >&2
    exit 2
fi

if [[ $mode == apk ]]; then
    sdk=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}
    platform=${ANDROID_JAR:-$sdk/platforms/android-34/android.jar}
    tools=${ANDROID_BUILD_TOOLS:-$sdk/build-tools/34.0.0}
    if [[ ! -f $platform || ! -x $tools/aapt || ! -x $tools/apksigner ]]; then
        echo "Android platform and build tools required. See README.md." >&2
        exit 1
    fi
fi

mkdir -p build/deps
fetch() {
    local url=$1 destination=build/deps/${1##*/}
    if [[ ! -s $destination ]]; then
        curl --fail --silent --show-error --location "$url" -o "$destination.part"
        mv "$destination.part" "$destination"
    fi
}

central=https://repo.maven.apache.org/maven2
bc_version=1.86
for artifact in bcprov bcutil bctls; do
    name=$artifact-jdk15to18
    fetch "$central/org/bouncycastle/$name/$bc_version/$name-$bc_version.jar"
done
ecj=3.26.0
fetch "$central/org/eclipse/jdt/ecj/$ecj/ecj-$ecj.jar"
classpath="build/deps/bcprov-jdk15to18-$bc_version.jar:build/deps/bcutil-jdk15to18-$bc_version.jar:build/deps/bctls-jdk15to18-$bc_version.jar"

mkdir -p "build/$mode/classes"
compiler=(-1.5 -proc:none -d "build/$mode/classes" -classpath "$classpath")
sources=(src/linka/tlsdemo/TlsHello.java)
if [[ $mode == apk ]]; then
    compiler+=(-bootclasspath "$platform")
    sources+=(src/linka/tlsdemo/DemoActivity.java)
fi
java -jar "build/deps/ecj-$ecj.jar" "${compiler[@]}" "${sources[@]}"
if [[ $mode == desktop ]]; then
    exit 0
fi

# Remove unused BC code and relocate reachable classes away from Android's built-in BC.
r8="$tools/lib/d8.jar"
mkdir -p build/apk/dex
jar cf build/apk/classes.jar -C build/apk/classes .
IFS=: read -r -a libraries <<< "$classpath"
java -cp "$r8" com.android.tools.r8.R8 --release --min-api 5 \
    --lib "$platform" --pg-conf r8.pro --output build/apk/dex \
    build/apk/classes.jar "${libraries[@]}"

"$tools/aapt" package -f -M AndroidManifest.xml -I "$platform" -F build/apk/unsigned.apk
zip -jq build/apk/unsigned.apk build/apk/dex/classes.dex
"$tools/zipalign" -f 4 build/apk/unsigned.apk build/apk/aligned.apk

if [[ ! -f build/apk/demo.keystore ]]; then
    keytool -genkeypair -keystore build/apk/demo.keystore -storetype JKS \
        -storepass android -keypass android -alias androiddebugkey \
        -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Linka TLS Demo'
fi
"$tools/apksigner" sign --min-sdk-version 5 --v1-signing-enabled true \
    --v2-signing-enabled false --v3-signing-enabled false --v4-signing-enabled false \
    --ks build/apk/demo.keystore --ks-pass pass:android \
    --out build/tls-app-demo.apk build/apk/aligned.apk
"$tools/apksigner" verify --min-sdk-version 5 --verbose build/tls-app-demo.apk
