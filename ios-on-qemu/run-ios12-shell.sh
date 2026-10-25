#!/usr/bin/env bash
#
# run-ios12-shell.sh - path D: iOS 12.1 (16B92) on an emulated iPhone 6s Plus.
#
# Emulator: alephsecurity/xnu-qemu-arm64. Boots the real iOS 12.1 kernel to an
# interactive root bash (bash-4.4#). There is NO home screen and there never
# will be with this fork: launchd does start SpringBoard and backboardd, but the
# machine model has no display controller (CLCD/DCP), no GPU (AGX/Metal), no
# touch (multitouch/SPI) and no HID keyboard, so nothing ever opens a
# framebuffer user client. The ramfb is only XNU's early text console.
#
# This script launches only. It contains no Apple firmware and downloads
# nothing; the five prepared artifacts must already exist.
#
#   ./run-ios12-shell.sh                # window shows the framebuffer
#   ./run-ios12-shell.sh --console      # everything in this terminal
#   ./run-ios12-shell.sh --ssh 'uname -a'
#
set -eu

HERE="$(cd "$(dirname "$0")" && pwd)"
BUILD="${XNU_BUILD:-${HERE}/xnu-qemu-arm64/build}"
QEMU="${XNU_QEMU:-${HERE}/xnu-qemu-arm64/aarch64-softmmu/qemu-system-aarch64}"
MODE="screen"
SSH_CMD=""

usage() { sed -n '2,22p' "$0"; exit 0; }
while [ $# -gt 0 ]; do
  case "$1" in
    --build)   BUILD="$2"; shift 2 ;;
    --qemu)    QEMU="$2"; shift 2 ;;
    --console) MODE="console"; shift ;;
    --ssh)     MODE="ssh"; SSH_CMD="${2:-}"; shift; [ $# -gt 0 ] && shift || true ;;
    -h|--help) usage ;;
    *) echo "unknown option: $1" >&2; exit 64 ;;
  esac
done

# ---------------------------------------------------------------- SSH mode
if [ "$MODE" = "ssh" ]; then
  command -v sshpass >/dev/null 2>&1 || { echo "ssh mode needs sshpass" >&2; exit 1; }
  # Aleph's tcp-tunnel forwards host 2222 -> guest 22 (dropbear). It comes up
  # roughly 25 s after QEMU starts.
  if [ -n "$SSH_CMD" ]; then
    exec sshpass -p alpine ssh -p 2222 -o StrictHostKeyChecking=no \
      -o UserKnownHostsFile=/dev/null root@127.0.0.1 "$SSH_CMD"
  fi
  exec sshpass -p alpine ssh -p 2222 -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null root@127.0.0.1
fi

# ------------------------------------------------------------ preflight
[ -x "$QEMU" ] || { cat >&2 <<EOF
[xnu-qemu] no emulator at: $QEMU

Build alephsecurity/xnu-qemu-arm64 with gcc, and on a Linux host apply the
socket-layer patches from eugenesflanagin-spec/ios-emu-linux first: the fork's
guest-services layer assumes macOS (Darwin sockaddr layout, the value of
O_NONBLOCK, errno numbers) and its stock ramfb advertises 16 bpp while drawing
24 bpp r8g8b8, which shows up as magenta text.
Override with --qemu /path/to/qemu-system-aarch64
EOF
exit 1; }

cd "$BUILD"
REQUIRED=(
  "kernelcache.release.n66.out"      # decoded + lzss-decompressed kernelcache (Mach-O arm64)
  "DeviceTree.n66ap.im4p.out"        # decoded device tree
  "aleph_bdev_drv.bin"               # custom block-device driver, self-built
  "hfs.main"                         # 6 GB HFS+ root fs (+ iosbinpack64, LaunchDaemons)
  "hfs.sec"                          # 2 GB HFS+ read/write disk (/private/var, dropbear)
)
missing=0
for f in "${REQUIRED[@]}"; do
  if [ -f "$f" ]; then
    printf '  ok       %-34s %s\n' "$f" "$(du -h "$f" | cut -f1)"
  else
    printf '  MISSING  %-34s\n' "$f" >&2
    missing=1
  fi
done
if [ "$missing" = 1 ]; then
  cat >&2 <<EOF

[xnu-qemu] looked in: $BUILD
Prepare these per Aleph's guide (wiki: Build-iOS-on-QEMU). On Linux the
macOS-only steps have replacements - see ios-emu-linux/ios12-xnu-qemu/README.md:
  * kernelcache / device tree / ramdisk: ASN.1 (im4p) decode, then lzss or lzfse
  * hfs.main and hfs.sec: built from the iOS 12.1 rootfs + the iosbinpack64
    binaries + a bash/dropbear LaunchDaemon, then patched fstab and no keybagd
  * aleph_bdev_drv.bin: built from xnu-qemu-arm64-tools
No Apple firmware ships with this repository.
EOF
  exit 2
fi

# hfs.sec is mounted read/write by iOS and is never cleanly unmounted when QEMU
# is killed, so repair it before every start.
if command -v fsck.hfsplus >/dev/null 2>&1; then
  echo "[xnu-qemu] repairing hfs.sec ..."
  fsck.hfsplus -fy "$BUILD/hfs.sec" >/dev/null 2>&1 || true
fi

if [ "$MODE" = "console" ]; then
  echo "[xnu-qemu] serial-only mode: everything in this terminal."
  DISPLAY_ARGS=(-nographic)
  RAMFB="xnu-ramfb=off"
else
  echo "[xnu-qemu] screen mode: the window shows the iOS text framebuffer"
  echo "[xnu-qemu] (verbose boot, then the root shell). THIS terminal is the"
  echo "[xnu-qemu] keyboard - what you type appears in the window."
  echo "[xnu-qemu] after ~70 s press Enter once for the bash-4.4# prompt."
  DISPLAY_ARGS=(-display gtk)
  RAMFB="xnu-ramfb=on"
fi

cat <<'EOF'
[xnu-qemu] inside the guest shell:
    export PATH=$PATH:/iosbinpack64/usr/bin:/iosbinpack64/bin:/iosbinpack64/usr/sbin:/iosbinpack64/sbin
[xnu-qemu] quit QEMU with Ctrl-a x (Ctrl-a c = monitor). Shut the guest down
           properly if you care about what it wrote: the root fs is read-only,
           hfs.sec is not, and killing QEMU can leave it inconsistent.
[xnu-qemu] SSH from another terminal ~25 s after start:  ./run-ios12-shell.sh --ssh
EOF

exec "$QEMU" \
  -M "iPhone6splus-n66-s8000,kernel-filename=kernelcache.release.n66.out,dtb-filename=DeviceTree.n66ap.im4p.out,driver-filename=aleph_bdev_drv.bin,qc-file-0-filename=hfs.main,qc-file-1-filename=hfs.sec,kern-cmd-args=debug=0x8 kextlog=0xfff cpus=1 rd=disk0 serial=2,$RAMFB" \
  -cpu max \
  -m 6G \
  -name "iOS 12.1 - iPhone 6s Plus (QEMU)" \
  -serial mon:stdio \
  "${DISPLAY_ARGS[@]}"
