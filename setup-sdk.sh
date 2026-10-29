#!/bin/sh
set -eu
cd "$(dirname "$0")"
SDK="${CACA_SDK_ROOT:-$PWD/.work/honeycomb-sdk}"
mkdir -p "$SDK"
cd "$SDK"
case "$(uname -s)" in
  Darwin) url=https://dl.google.com/android/repository/f6d24b187cc6bd534c6c37604205171784ac5621.build-tools_r30.0.3-macosx.zip; checksum=5741d5071ffd9c19a55c86cb6b6097b06cf3897b64f14b3886ab523f287067c6 ;;
  Linux) url=https://dl.google.com/android/repository/build-tools_r30.0.3-linux.zip; checksum=24593500aa95d2f99fb4f10658aae7e65cb519be6cd33fa164f15f27f3c4a2d6 ;;
  *) echo 'Use Linux x86_64 or macOS (Rosetta on Apple Silicon).' >&2; exit 1 ;;
esac
fetch() {
  dest="$1"; expected="$2"; source_url="$3"
  if [ ! -f "$dest" ]; then curl -fL --retry 3 --connect-timeout 20 --max-time 300 "$source_url" -o "$dest.part"; mv "$dest.part" "$dest"; fi
  python3 - "$dest" "$expected" <<'VERIFY'
import hashlib, pathlib, sys
p = pathlib.Path(sys.argv[1])
if hashlib.sha256(p.read_bytes()).hexdigest() != sys.argv[2]:
    raise SystemExit('Checksum mismatch: ' + str(p))
VERIFY
}
fetch build-tools.zip "$checksum" "$url"
fetch platform.zip ff6b26ad34d7060a72ba504b0314cef8ba3138005561705adec5ad470a073d9b https://dl.google.com/android/repository/android-3.2_r01-linux.zip
fetch r8.jar 59753e70a74f918389cc87f1b7d66b5c0862932559167425708ded159e3de439 https://dl.google.com/dl/android/maven2/com/android/tools/r8/8.3.37/r8-8.3.37.jar
if [ ! -f build-tools/android-11/aapt ]; then unzip -q build-tools.zip -d build-tools; fi
if [ ! -f platform/android-3.2_r01-linux/android.jar ]; then unzip -q platform.zip -d platform; fi
