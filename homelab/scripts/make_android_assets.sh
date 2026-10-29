#!/usr/bin/env bash
# Генерация иконок и сплэш-экранов Android из одной картинки (ImageMagick)
# Запуск: ./scripts/make_android_assets.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ICON="$ROOT/public/art/icon.png"
RES="$ROOT/android/app/src/main/res"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

[ -f "$ICON" ] || { echo "нет $ICON"; exit 1; }

# ── Скруглённая иконка-плитка ────────────────────────────────────────
rounded() { # src size out
  local s="$2" r
  r=$(( s / 5 ))
  convert "$1" -resize "${s}x${s}!" -alpha set \
    \( -size "${s}x${s}" xc:black -fill white -draw "roundrectangle 0,0 $((s-1)),$((s-1)) $r,$r" \) \
    -alpha off -compose CopyOpacity -composite "$3"
}

# ── Обычные иконки приложения ────────────────────────────────────────
declare -A SIZE=( [mdpi]=48 [hdpi]=72 [xhdpi]=96 [xxhdpi]=144 [xxxhdpi]=192 )
declare -A FG=( [mdpi]=108 [hdpi]=162 [xhdpi]=216 [xxhdpi]=324 [xxxhdpi]=432 )

for d in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
  s="${SIZE[$d]}"; f="${FG[$d]}"
  mkdir -p "$RES/mipmap-$d"
  convert "$ICON" -resize "${s}x${s}" -strip "$RES/mipmap-$d/ic_launcher.png"
  convert "$ICON" -resize "${s}x${s}" -alpha set \
    \( -size "${s}x${s}" xc:black -fill white -draw "circle $((s/2)),$((s/2)) $((s/2)),1" \) \
    -alpha off -compose CopyOpacity -composite -strip "$RES/mipmap-$d/ic_launcher_round.png"
  g=$(( f * 60 / 100 ))
  rounded "$ICON" "$g" "$TMP/fg-${d}.png"
  convert -size "${f}x${f}" xc:none "$TMP/fg-${d}.png" -gravity center -composite \
    -strip "$RES/mipmap-$d/ic_launcher_foreground.png"
done

# ── Adaptive-иконка (Android 8+): фон-градиент + глиф ────────────────
mkdir -p "$RES/drawable" "$RES/mipmap-anydpi-v26"
cat > "$RES/drawable/ic_launcher_background.xml" <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient
        android:angle="135"
        android:startColor="#0f2b3d"
        android:centerColor="#0b1a26"
        android:endColor="#05090d"
        android:type="linear" />
</shape>
XML
for name in ic_launcher ic_launcher_round; do
  cat > "$RES/mipmap-anydpi-v26/$name.xml" <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
XML
done

# ── Сплэш-экраны: тёмный градиент, свечение, иконка, название ───────
# glow-подложка (прозрачная, с размытием)
convert -size 560x560 xc:none -fill 'rgba(62,224,138,0.22)' -draw "circle 280,280 280,80" -blur 0x70 "$TMP/glow.png"

splash() { # w h out
  local w="$1" h="$2" out="$3" isz logo sub
  isz=$(( w * 30 / 100 )); logo=$(( w * 8 / 100 )); sub=$(( w * 3 / 100 ))
  (( logo < 22 )) && logo=22
  (( sub < 9 )) && sub=9
  if (( w >= h )); then
    isz=$(( h * 34 / 100 )); logo=$(( h * 9 / 100 )); sub=$(( h * 4 / 100 ))
    (( logo < 22 )) && logo=22
    (( sub < 9 )) && sub=9
  fi

  rounded "$ICON" "$isz" "$TMP/icon-$w-$h.png"
  convert "$TMP/glow.png" -resize "$((isz * 2))x$((isz * 2))" "$TMP/glow-$w-$h.png"

  convert -size "${w}x${h}" gradient:'#0d1e2a-#04070b' \
    \( "$TMP/glow-$w-$h.png" \) -gravity center -geometry "+0-$(( h * 12 / 100 ))" -composite \
    \( "$TMP/icon-$w-$h.png" \) -gravity center -geometry "+0-$(( h * 12 / 100 ))" -composite \
    -gravity center -fill '#3ee08a' -font DejaVu-Sans-Bold -pointsize "$logo" \
    -annotate "+0+$(( h * 2 / 100 ))" 'HomeLab' \
    -gravity center -fill '#6f8497' -font DejaVu-Sans-Mono -pointsize "$sub" \
    -annotate "+0+$(( h * 9 / 100 ))" 'от Авито до Дата-Центра' \
    -depth 8 -strip "$out"
}

# портрет/ландшафт по стандартным плотностям
splash  320  480 "$RES/drawable-port-mdpi/splash.png"
splash  480  800 "$RES/drawable-port-hdpi/splash.png"
splash  720 1280 "$RES/drawable-port-xhdpi/splash.png"
splash  960 1600 "$RES/drawable-port-xxhdpi/splash.png"
splash 1280 1920 "$RES/drawable-port-xxxhdpi/splash.png"
splash  480  320 "$RES/drawable-land-mdpi/splash.png"
splash  800  480 "$RES/drawable-land-hdpi/splash.png"
splash 1280  720 "$RES/drawable-land-xhdpi/splash.png"
splash 1600  960 "$RES/drawable-land-xxhdpi/splash.png"
splash 1920 1280 "$RES/drawable-land-xxxhdpi/splash.png"
splash 1080 1920 "$RES/drawable/splash.png"

# ── Иконки для PWA (public/icons) ───────────────────────────────────
mkdir -p "$ROOT/public/icons"
for s in 48 72 96 144 192 512; do
  convert "$ICON" -resize "${s}x${s}" -strip "$ROOT/public/icons/icon-${s}.png"
done

echo "✓ Иконки и сплэши обновлены в $RES"
