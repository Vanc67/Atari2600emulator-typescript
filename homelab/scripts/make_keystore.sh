#!/usr/bin/env bash
# Демонстрационный ключ подписи для сборки APK.
#
# ВАЖНО: этот ключ лежит в открытом репозитории — он годится только для
# личной сборки и установки на свои устройства. Для Google Play создайте
# собственный ключ и не публикуйте его:
#
#   ./scripts/make_keystore.sh my-release.jks "сложный-пароль"
#
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NAME="${1:-homelab-demo.jks}"
PASS="${2:-homelab123}"
ALIAS="homelab"
OUT="$ROOT/android/keystore/$NAME"

mkdir -p "$ROOT/android/keystore"

if [ -f "$OUT" ]; then
  echo "Ключ уже существует: $OUT"
elif command -v keytool >/dev/null 2>&1; then
  keytool -genkeypair \
    -keystore "$OUT" \
    -alias "$ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10950 \
    -storepass "$PASS" -keypass "$PASS" \
    -dname "CN=HomeLab, OU=Game, O=HomeLab, L=Kirov, S=Kirov, C=RU" \
    -storetype PKCS12
  echo "✓ Создан ключ $OUT (alias=$ALIAS)"
else
  # keytool недоступен (нет JDK) — собираем PKCS12 тем же алгоритмом через openssl
  TMP="$(mktemp -d)"
  openssl req -x509 -newkey rsa:2048 -nodes -days 10950 \
    -subj "/CN=HomeLab/OU=Game/O=HomeLab/L=Kirov/S=Kirov/C=RU" \
    -keyout "$TMP/key.pem" -out "$TMP/cert.pem" >/dev/null 2>&1
  openssl pkcs12 -export \
    -inkey "$TMP/key.pem" -in "$TMP/cert.pem" \
    -name "$ALIAS" -out "$OUT" \
    -passout "pass:$PASS" \
    -keypbe AES-256-CBC -certpbe AES-256-CBC -macalg sha256
  rm -rf "$TMP"
  echo "✓ Создан ключ $OUT через openssl (alias=$ALIAS)"
fi

cat > "$ROOT/android/keystore.properties" <<PROPS
# Подпись release-сборки (используется в android/app/build.gradle)
storeFile=keystore/$NAME
storePassword=$PASS
keyAlias=$ALIAS
keyPassword=$PASS
PROPS
echo "✓ Записан android/keystore.properties"
