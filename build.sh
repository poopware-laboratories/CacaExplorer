#!/bin/sh
set -eu
cd "$(dirname "$0")"
. ./tools/java-env.sh
SDK="${CACA_SDK_ROOT:-$PWD/.work/honeycomb-sdk}"
TOOLS="${ANDROID_BUILD_TOOLS:-$SDK/build-tools/android-11}"
PLATFORM="${ANDROID_JAR:-$SDK/platform/android-3.2_r01-linux/android.jar}"
R8="${CACA_R8_JAR:-$SDK/r8.jar}"
KEY="${CACA_KEYSTORE:-$PWD/.private/debug.keystore}"
VERSION=$(python3 -c 'import xml.etree.ElementTree as E; print(E.parse("AndroidManifest.xml").getroot().attrib["{http://schemas.android.com/apk/res/android}versionName"])')
APK="dist/CacaExplorer-$VERSION.apk"
export CACA_KEYSTORE_PASSWORD="${CACA_KEYSTORE_PASSWORD:-android}"
export CACA_KEY_PASSWORD="${CACA_KEY_PASSWORD:-$CACA_KEYSTORE_PASSWORD}"
if [ ! -f "$R8" ] || [ ! -f "$PLATFORM" ] || [ ! -x "$TOOLS/aapt" ]; then
    echo 'Missing SDK dependencies. Run sh setup-sdk.sh first.' >&2
    exit 1
fi
rm -rf build/classes build/dex build/generated
mkdir -p build/classes build/dex build/generated .private dist
"$TOOLS/aapt" package -f -M AndroidManifest.xml -S res -I "$PLATFORM" -J build/generated -F build/unsigned.apk
python3 - <<'INFO'
import json, pathlib
name = json.load(open('version.json'))['codename']
path = pathlib.Path('build/generated/org/poopware/cacaexplorer/BuildInfo.java')
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text('package org.poopware.cacaexplorer; final class BuildInfo { static final String CODENAME = ' + json.dumps(name) + '; }')
INFO
find src build/generated -name '*.java' > build/sources.txt
javac -source 8 -target 8 -bootclasspath "$PLATFORM" -d build/classes @build/sources.txt
jar cf build/classes.jar -C build/classes .
java -cp "$R8" com.android.tools.r8.D8 --min-api 13 --lib "$PLATFORM" --output build/dex build/classes.jar
(cd build/dex && zip -q -X ../unsigned.apk classes.dex)
"$TOOLS/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [ ! -f "$KEY" ]; then
    if [ -n "${CACA_KEYSTORE:-}" ]; then echo 'Configured signing key is missing.' >&2; exit 1; fi
    (umask 077; keytool -genkeypair -keystore "$KEY" -storepass:env CACA_KEYSTORE_PASSWORD -keypass:env CACA_KEY_PASSWORD -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=CacaExplorer Development')
fi
"$TOOLS/apksigner" sign --ks "$KEY" --ks-key-alias "${CACA_KEY_ALIAS:-androiddebugkey}" --ks-pass env:CACA_KEYSTORE_PASSWORD --key-pass env:CACA_KEY_PASSWORD --min-sdk-version 13 --v1-signing-enabled true --v2-signing-enabled false --v3-signing-enabled false --out "$APK" build/aligned.apk
"$TOOLS/apksigner" verify --verbose --min-sdk-version 13 "$APK"
"$TOOLS/aapt" dump badging "$APK"
python3 - "$APK" <<'HASH'
import hashlib, pathlib, sys
p = pathlib.Path(sys.argv[1])
p.with_suffix(p.suffix + '.sha256').write_text(hashlib.sha256(p.read_bytes()).hexdigest() + '  ' + p.name + '\n')
HASH
