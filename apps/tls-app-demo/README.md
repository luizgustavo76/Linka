# Linka TLS demo

The Android activity targets API 5 (Android 2.0). It uses the Java 1.5 build
of Bouncy Castle's lightweight bctls API for TLS 1.3 and sends HTTP/1.1
GET requests. The client and HTTPS server require X25519 key exchange and
`TLS_CHACHA20_POLY1305_SHA256`. Both buttons use a fresh TCP connection and TLS handshake
for every request. The 50-call run is sequential and reports each result
and the total elapsed time.

## Start the server

From the repository root, generate a disposable certificate and note
its SHA-256 fingerprint (the value after the equals sign).

```sh
mkdir -p apps/tls-app-demo/build
openssl req -x509 -newkey rsa:2048 -nodes -sha256 -days 7 -subj /CN=localhost \
  -keyout apps/tls-app-demo/build/demo-key.pem \
  -out apps/tls-app-demo/build/demo-cert.pem
openssl x509 -in apps/tls-app-demo/build/demo-cert.pem -noout -sha256 -fingerprint
```

```sh
LINKA_DEMO_CERT=apps/tls-app-demo/build/demo-cert.pem \
LINKA_DEMO_KEY=apps/tls-app-demo/build/demo-key.pem \
cargo run -p linka-server -- --hello-demo
```

The demo listens on `127.0.0.1:8443` and serves `Hello, world!` at
`/hello` without initializing PostgreSQL, Redis, or S3. Omit both TLS
variables to serve plain HTTP for curl checks. The app expects HTTPS
and authenticates the endpoint by matching its exact leaf certificate
fingerprint. It expects the small, Content-Length-framed Axum response.

## Build and run on Android

The command-line build needs a JDK, curl, zip, and an Android SDK with
platform `android-34` and build-tools `34.0.0`. Set `ANDROID_HOME` if your
SDK lives outside `~/Android/Sdk`. You can point `ANDROID_JAR` at an API 5
platform jar to compile the activity against that platform directly.
The build uses ECJ to emit Java 1.5 class files and R8 to shrink and
relocate the packaged Bouncy Castle classes before producing Dalvik bytecode.
It signs the APK with a local development key using v1 signing.

```sh
cd apps/tls-app-demo
./build.sh
adb install -r build/tls-app-demo.apk
```

Launch **Linka TLS Demo**, enter the certificate fingerprint, and tap
**Make 1 call** or **Make 50 calls**. The default URL uses `10.0.2.2` to reach
the emulator host. For a physical device, set `LINKA_DEMO_ADDR` to the
host's LAN address and port when starting the server, then enter the
corresponding URL in the app.

## Smoke test without the Android SDK

With the HTTPS server running, execute the same TLS client from a desktop
JVM. Replace `CERT_SHA256` with the fingerprint above and use `1` or `50`
as the final argument.

```sh
cd apps/tls-app-demo
./build.sh desktop
java -cp 'build/desktop/classes:build/deps/*' linka.tlsdemo.TlsHello \
  https://127.0.0.1:8443/hello CERT_SHA256 50
```
