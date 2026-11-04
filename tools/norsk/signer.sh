#!/usr/bin/env bash
# signer.sh [adb-enhet] — hent siste APK fra «APK (norsk)», signer med nøkkelen i .auth/ og installer.
# Nøkkelen lages første gang og må aldri mistes (ny nøkkel = avinstaller appen og last ned modellene på nytt).
set -euo pipefail
cd "$(dirname "$0")/../.."
REPO=hakonamatata177/marmalade-stt-android
ENHET=${1:-$(adb devices | awk '$2=="device"{print $1}' | while read -r d; do
  [ "$(adb -s "$d" shell getprop ro.product.model 2>/dev/null | tr -d '\r')" = "Pixel 10 Pro" ] && echo "$d"; done | head -1)}
KS=.auth/norsk.keystore; PW=.auth/keystore_pw
mkdir -p .auth build/norsk && chmod 700 .auth

run=$(gh run list -R "$REPO" -w "APK (norsk)" -s success -L 1 --json databaseId,headSha -q '.[0].databaseId')
[ -n "$run" ] || { echo "Ingen vellykket APK-kjøring ennå"; exit 1; }
rm -rf build/norsk/* && gh run download "$run" -R "$REPO" -n apk-norsk -D build/norsk
apk=$(ls build/norsk/*arm64-v8a*-unsigned.apk)

if [ ! -f "$KS" ]; then
  head -c 24 /dev/urandom | base64 | tr -d '/+=' > "$PW"; chmod 600 "$PW"
  keytool -genkeypair -keystore "$KS" -storepass "$(cat "$PW")" -keypass "$(cat "$PW")" -alias norsk \
    -keyalg RSA -keysize 4096 -validity 36500 -dname "CN=Marmalade Norsk" >/dev/null
fi
zipalign -c -p 4 "$apk"
apksigner sign --ks "$KS" --ks-pass "file:$PW" --ks-key-alias norsk --out build/norsk/marmalade-norsk.apk "$apk"
apksigner verify build/norsk/marmalade-norsk.apk
echo "Kjøring $run → build/norsk/marmalade-norsk.apk"
if [ -n "$ENHET" ]; then adb -s "$ENHET" install -r build/norsk/marmalade-norsk.apk; else echo "Fant ingen Pixel over adb; installer for hånd."; fi
