#!/usr/bin/env bash
#
# run-ipod-touch-1g.sh - path C: iPod touch 1G, iPhoneOS 1.0, full GUI.
#
# Emulator: devos50/qemu-ios (active fork: samhenrigold/qemu-ios).
# This script is a launcher only. It contains no Apple firmware and downloads
# nothing: it expects the four system files you obtained from the project's own
# release (n45ap_v1) to already be on disk.
#
#   ./run-ipod-touch-1g.sh                 # auto-detect dirs
#   ./run-ipod-touch-1g.sh --help
#   IT_FILES=~/ipod-files IT_QEMU=~/qemu-ios/build ./run-ipod-touch-1g.sh
#
# Controls (SDL window focused): mouse click/drag = finger, H = Home,
# hold P ~5 s = power off. NEVER short-press P - the device does not wake from
# sleep. Changes are not kept between boots; that is how this emulator works.
#
set -eu

HERE="$(cd "$(dirname "$0")" && pwd)"

# Where the four system files live (bootrom, NOR image, NAND zip/dir).
FILES="${IT_FILES:-${HOME}/.local/share/qemu-ios-ipod}"
# Where the built emulator lives.
BUILD="${IT_QEMU:-${HERE}/qemu-ios/build}"
# Machine name: samhenrigold's fork uses iPod-Touch-1G; upstream's RUNNING.md
# (the iPod touch 2G guide) uses iPod-Touch. Override if your build differs.
MACHINE="${IT_MACHINE:-iPod-Touch-1G}"
MEM="${IT_MEM:-2G}"
NAND_TMP="${IT_NAND_TMP:-/dev/shm/ipod-nand}"
EXTRA_ARGS=()

usage() { sed -n '2,20p' "$0"; exit 0; }

while [ $# -gt 0 ]; do
  case "$1" in
    --files)   FILES="$2"; shift 2 ;;
    --qemu)    BUILD="$2"; shift 2 ;;
    --machine) MACHINE="$2"; shift 2 ;;
    --mem)     MEM="$2"; shift 2 ;;
    --console) EXTRA_ARGS+=("-nographic"); shift ;;
    --verbose) EXTRA_ARGS+=("-d" "unimp"); shift ;;
    -h|--help) usage ;;
    *) echo "unknown option: $1" >&2; exit 64 ;;
  esac
done

QEMU=""
for candidate in \
  "$BUILD/arm-softmmu/qemu-system-arm" \
  "$BUILD/qemu-system-arm" \
  "$(command -v qemu-system-arm 2>/dev/null || true)"
do
  if [ -n "$candidate" ] && [ -x "$candidate" ]; then QEMU="$candidate"; break; fi
done
if [ -z "$QEMU" ]; then
  cat >&2 <<EOF
[qemu-ios] no qemu-system-arm found.

Looked in:
  $BUILD/arm-softmmu/qemu-system-arm
  $BUILD/qemu-system-arm
  \$PATH

Build the fork first (gcc, not clang):
  git clone https://github.com/devos50/qemu-ios.git && cd qemu-ios
  git checkout ipod_touch_1g
  mkdir build && cd build
  ../configure --enable-sdl --target-list=arm-softmmu
  make -j\$(nproc)
On Apple Silicon macOS add: --disable-capstone --disable-pie --disable-slirp \\
  --disable-fuse and point --extra-cflags/--extra-ldflags at openssl@3.
On recent macOS, if configure says "Ouch! ... found no usable distlib", give it
a python that has distlib via --python=.
Then re-run with --qemu /path/to/qemu-ios/build
EOF
  exit 1
fi

# --- locate the system files ---------------------------------------------
find_one() {  # find_one <glob-name> -> prints path or empty
  local pattern="$1" hit
  for hit in "$FILES"/$pattern; do
    [ -e "$hit" ] && { echo "$hit"; return 0; }
  done
  return 1
}

BOOTROM="$(find_one '*bootrom*' || find_one '*BootROM*' || true)"
NOR="$(find_one 'nor_*.bin' || find_one '*.nor' || true)"
NAND_DIR=""
NAND_ZIP="$(find_one 'nand_*.zip' || true)"

if [ -n "$NAND_ZIP" ]; then
  # The NAND is ~133,000 tiny files; on a spinning disk that is unusably slow,
  # so the upstream launcher unpacks it into tmpfs. We do the same.
  if [ -d /dev/shm ] && [ "$(uname -s)" = "Linux" ]; then
    echo "[qemu-ios] unpacking NAND into tmpfs ($NAND_TMP) ..."
    rm -rf "$NAND_TMP"; mkdir -p "$NAND_TMP"
    unzip -q -o "$NAND_ZIP" -d "$NAND_TMP"
    # the zip contains a single top-level directory named "nand"
    if [ -d "$NAND_TMP/nand" ]; then NAND_DIR="$NAND_TMP/nand"; else NAND_DIR="$NAND_TMP"; fi
  else
    echo "[qemu-ios] no tmpfs; using the zip's directory directly (slow)"
    NAND_DIR="$(dirname "$NAND_ZIP")/nand"
  fi
elif [ -d "$FILES/nand" ]; then
  NAND_DIR="$FILES/nand"
fi

missing=0
[ -n "$BOOTROM" ] && [ -f "$BOOTROM" ] || { echo "[qemu-ios] MISSING: bootrom binary (S5L8900)" >&2; missing=1; }
[ -n "$NOR" ]     && [ -f "$NOR" ]     || { echo "[qemu-ios] MISSING: NOR image (nor_n45ap.bin)" >&2; missing=1; }
[ -n "$NAND_DIR" ] && [ -d "$NAND_DIR" ] || { echo "[qemu-ios] MISSING: NAND directory (cs0-cs3)" >&2; missing=1; }

if [ "$missing" = 1 ]; then
  cat >&2 <<EOF

[qemu-ios] looked in: $FILES
Expected the four system files from the project's own release (n45ap_v1):
  * the S5L8900 bootrom binary   - started first, as on real hardware
  * a NOR image                  - auxiliary bootloader files
  * a NAND image (nand_*.zip)    - contains the root filesystem
  * (2G guide also references a NOR *directory* layout; this fork takes a file)

This repository ships no Apple firmware. Get those files from the emulator
project's release page, or generate them from a stock IPSW with
devos50/qemu-ios-generate-nor and devos50/qemu-ios-generate-nand.
Point the script at them with --files DIR or IT_FILES=DIR.
EOF
  exit 2
fi

# --- SDL / display quirks -------------------------------------------------
if [ "$(uname -s)" = "Linux" ] && [ -n "${WAYLAND_DISPLAY:-}" ]; then
  # SDL3's sdl2-compat hangs on X11; the upstream launcher forces Wayland.
  export SDL_VIDEODRIVER="${SDL_VIDEODRIVER:-wayland}"
  echo "[qemu-ios] forcing SDL_VIDEODRIVER=wayland"
fi

DISPLAY_ARGS=(-display sdl)
[ ${#EXTRA_ARGS[@]} -gt 0 ] && [ "${EXTRA_ARGS[0]}" = "-nographic" ] && DISPLAY_ARGS=(-nographic)

echo "[qemu-ios] emulator : $QEMU"
echo "[qemu-ios] machine  : $MACHINE   mem: $MEM"
echo "[qemu-ios] bootrom  : $BOOTROM"
echo "[qemu-ios] nor      : $NOR"
echo "[qemu-ios] nand     : $NAND_DIR"
echo "[qemu-ios] keys     : mouse=finger  H=Home  hold P ~5s=power off"
echo "[qemu-ios] note     : changes are not kept between boots"

exec "$QEMU" \
  -M "$MACHINE,bootrom=$BOOTROM,nand=$NAND_DIR,nor=$NOR" \
  -serial mon:stdio \
  -cpu max \
  -m "$MEM" \
  "${DISPLAY_ARGS[@]}" \
  ${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}
