#!/usr/bin/env bash
#
# check-host.sh - can THIS machine run a real iOS in QEMU, and which one?
#
# Read-only: it inspects the system, it installs nothing and downloads nothing.
# Every threshold below comes from the upstream projects' own documentation
# (see SOURCES.md), not from guesswork.
#
#   ./check-host.sh              # report for all four paths
#   ./check-host.sh --path A     # one path only (A/B/C/D)
#   ./check-host.sh --disk /mnt  # measure free space on another mount
#
set -u

DISK_DIR="$HOME"
ONLY=""
while [ $# -gt 0 ]; do
  case "$1" in
    --path) ONLY="$2"; shift 2 ;;
    --disk) DISK_DIR="$2"; shift 2 ;;
    -h|--help) sed -n '2,14p' "$0"; exit 0 ;;
    *) echo "unknown option: $1" >&2; exit 64 ;;
  esac
done

OS="$(uname -s)"
ARCH="$(uname -m)"
CORES="$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 0)"

# --- RAM in MB -------------------------------------------------------------
ram_mb() {
  if [ "$OS" = "Linux" ] && [ -r /proc/meminfo ]; then
    awk '/^MemTotal:/ {printf "%d", $2/1024; exit}' /proc/meminfo
  elif [ "$OS" = "Darwin" ]; then
    echo $(( $(sysctl -n hw.memsize 2>/dev/null || echo 0) / 1048576 ))
  else
    echo 0
  fi
}
RAM_MB="$(ram_mb)"

# --- free disk in MB -------------------------------------------------------
# df -Pmk reports 1K blocks, so the available column has to be scaled down.
free_mb() {
  df -Pmk "$1" 2>/dev/null | awk 'NR==2 {printf "%d", $4/1024}'
}
DISK_FREE_MB="$(free_mb "$DISK_DIR" || echo 0)"

have() { command -v "$1" >/dev/null 2>&1; }
pkg()  { have pkg-config && pkg-config --exists "$1" 2>/dev/null; }
header() { [ -e "/usr/include/$1" ] || [ -e "/usr/local/include/$1" ] || [ -e "/opt/homebrew/include/$1" ]; }

PASS=0; WARN=0; FAIL=0
ok()   { printf '    \033[32mPASS\033[0m  %s\n' "$1"; PASS=$((PASS+1)); }
warn() { printf '    \033[33mWARN\033[0m  %s\n' "$1"; WARN=$((WARN+1)); }
bad()  { printf '    \033[31mFAIL\033[0m  %s\n' "$1"; FAIL=$((FAIL+1)); }
info() { printf '    \033[36m....\033[0m  %s\n' "$1"; }

need_threads() { [ "$CORES" -ge "$1" ] && ok "$CORES CPU threads (need >= $1)" \
                                       || bad "$CORES CPU threads (need >= $1)"; }
need_ram()     { [ "$RAM_MB" -ge "$(( $1 * 1024 ))" ] && ok "$((RAM_MB/1024)) GB RAM (need >= $1 GB)" \
                                       || bad "$((RAM_MB/1024)) GB RAM (need >= $1 GB)"; }
need_disk()    { [ "${DISK_FREE_MB:-0}" -ge "$(( $1 * 1024 ))" ] \
                                       && ok "$((DISK_FREE_MB/1024)) GB free on $DISK_DIR (need >= $1 GB)" \
                                       || bad "$((DISK_FREE_MB/1024)) GB free on $DISK_DIR (need >= $1 GB)"; }
need_cmd()     { have "$1" && ok "found $1$( [ $# -gt 1 ] && echo " - $2")" || bad "missing $1$( [ $# -gt 1 ] && echo " - $2")"; }
need_pkg()     { pkg "$1" && ok "pkg-config finds $1 ($(pkg-config --modversion "$1"))" \
                           || bad "pkg-config cannot find $1 - install the dev package"; }
need_hdr()     { header "$1" && ok "headers present: $1" || bad "headers missing: $1"; }
opt_cmd()      { have "$1" && ok "found $1 (optional)" || warn "missing $1 (optional) - $2"; }

banner() { printf '\n\033[1m%s\033[0m\n' "$1"; }

echo "======================================================================"
echo " iOS-on-QEMU host check          $(date -u '+%Y-%m-%d %H:%M UTC')"
echo "======================================================================"
echo "  OS            : $OS $( [ "$OS" = Linux ] && uname -r )"
echo "  arch          : $ARCH"
echo "  CPU threads   : $CORES"
echo "  RAM           : $((RAM_MB/1024)) GB"
echo "  free on $DISK_DIR : $((DISK_FREE_MB/1024)) GB"
if [ -r /etc/os-release ]; then
  . /etc/os-release
  echo "  distro        : ${PRETTY_NAME:-unknown}"
fi
[ -n "${WAYLAND_DISPLAY:-}" ] && echo "  session       : Wayland" \
                              || echo "  session       : ${DISPLAY:+X11}${DISPLAY:-none}"

# ---------------------------------------------------------------------- A
path_A() {
banner "A. iPhone 11 / iOS 14 - ChefKiss Inferno (full GUI)"
info "real home screen, Settings, Safari, Photos; mouse = finger; ~2.5 min boot"
if [ "$OS" != "Linux" ] || [ "$ARCH" != "x86_64" ]; then
  warn "documented and tested on x86_64 Linux (you are on $OS/$ARCH)"
fi
need_threads 8
need_ram 16
need_disk 25
need_cmd gcc "build with gcc, NOT clang/aocc - clang trips over an LTO-built library"
need_cmd git "git submodule update --init is required"
need_cmd meson
need_cmd ninja
need_cmd pkg-config
need_pkg glib-2.0
need_pkg pixman-1
need_pkg gtk3
need_pkg libpng
need_pkg libusb-1.0
need_pkg sdl2
need_pkg libslirp
opt_cmd lzfse "only in the AUR on Arch; needed to build the t8030 package"
opt_cmd qemu-img "used for disk snapshots between working stages"
opt_cmd kdotool "KDE only: centres the VM window"
info "build note: keep debug info ON (-Ddebug=true); turning it off silently"
info "removes code the emulator needs and the restore then fails with"
info "'SEP returned zero-length ART'"
info "firmware: only iOS 14.0 beta 5 (18A5351d) for iPhone12,1, from Apple"
info "setup automation is deliberately NOT published upstream - follow"
info "https://chefkiss.dev/applehax/inferno/ by hand"
}

# ---------------------------------------------------------------------- B
path_B() {
banner "B. iPhone X / iOS 16.7.16 - lgrachov/ios-qemu (newest real iOS)"
info "lock + home screen in ~1 min, stock apps, Safari really browses the web"
if [ "$OS" = "Darwin" ] && [ "$ARCH" = "arm64" ]; then
  ok "Apple Silicon Mac (hvf available)"
else
  bad "needs an Apple Silicon Mac: hvf is mandatory (you are on $OS/$ARCH)"
fi
need_disk 40
need_cmd brew "Homebrew provides qemu / rsync / llvm / gh / ipsw"
opt_cmd qemu "Homebrew qemu, tested with 11.0.2"
opt_cmd gh "the kernel is built on GitHub Actions from your fork"
opt_cmd ipsw "blacktop/ipsw: names the OS/SystemOS/AppOS images and extracts im4p"
opt_cmd rsync "must be GNU rsync, not the BSD one that ships with macOS"
opt_cmd clang "Xcode CLT: clang assembles the boot stub"
info "the kernel is rebuilt from Apple's open-source xnu-8796, not taken from"
info "the IPSW; the IPSW supplies the iPhone X 16.7.16 (20H392) userland"
info "security is off in this configuration (permissive AMFI/Sandbox, stub"
info "AppleKeyStore, no data protection) - put nothing personal on it"
}

# ---------------------------------------------------------------------- C
path_C() {
banner "C. iPod touch 1G / iPhoneOS 1.0 - qemu-ios (the quick one)"
info "home screen, touch via mouse, built-in apps run; ~5 s boot; <1 h setup"
if [ "$OS" = "Linux" ] || [ "$OS" = "Darwin" ]; then
  ok "supported host ($OS)"
else
  bad "needs Linux or macOS (you are on $OS)"
fi
need_disk 2
need_cmd gcc "build the fork with gcc"
need_cmd git
need_cmd make
need_cmd pkg-config
need_pkg glib-2.0
need_pkg pixman-1
need_hdr zlib.h
opt_cmd sdl2-config "SDL is what shows the device screen (-display sdl)"
need_hdr SDL2/SDL.h
if have pkg-config && pkg-config --exists openssl; then
  ok "pkg-config finds openssl"
elif header openssl/evp.h; then
  ok "openssl headers present"
else
  bad "openssl dev headers missing - the AES/SHA1/PKE engines link against libcrypto"
fi
if [ "$OS" = "Linux" ]; then
  if [ -n "${WAYLAND_DISPLAY:-}" ]; then
    ok "Wayland session (SDL3's sdl2-compat hangs on X11)"
  else
    warn "no Wayland session detected - sdl2-compat hangs on X11, force Wayland"
  fi
  [ -d /dev/shm ] && ok "/dev/shm present (the NAND is ~133k small files)" \
                  || warn "no /dev/shm - unpacking the NAND on a HDD is painfully slow"
fi
info "firmware: four files from the project's own release (n45ap_v1):"
info "bootrom, NOR image, NAND directory. This repo ships none of them."
info "changes are NOT kept between boots, and the device never wakes from"
info "sleep - hold Power ~5 s to shut down, never short-press it"
}

# ---------------------------------------------------------------------- D
path_D() {
banner "D. iOS 12.1 / iPhone 6s Plus - alephsecurity xnu-qemu-arm64 (shell only)"
info "root bash-4.4# over serial + text framebuffer; NO home screen is possible"
if [ "$OS" = "Linux" ] || [ "$OS" = "Darwin" ]; then
  ok "supported host ($OS)"
else
  bad "needs Linux or macOS (you are on $OS)"
fi
need_ram 6
need_disk 15
need_cmd gcc
need_cmd python3 "the upstream bootstrap scripts are Python"
need_pkg glib-2.0
need_pkg pixman-1
opt_cmd fsck.hfsplus "hfs.sec is never cleanly unmounted when QEMU is killed"
opt_cmd sshpass "only for ssh-ios.sh (root/alpine on host port 2222)"
opt_cmd clang "only to cross-build the iOS side of the tcp tunnel without an Apple SDK"
opt_cmd ldid "signs that tunnel binary"
if [ "$OS" = "Linux" ]; then
  warn "on Linux you also need the ios-emu-linux socket patches: the fork's"
  warn "guest-services layer assumes a macOS host (sockaddr layout, O_NONBLOCK"
  warn "value, errno numbers) and the stock ramfb advertises 16 bpp but draws 24"
fi
info "artifacts you must build yourself: decoded kernelcache, decoded device"
info "tree, aleph_bdev_drv.bin, hfs.main (6 GB) and hfs.sec (2 GB)"
info "runs with -m 6G and a single emulated CPU"
}

for p in A B C D; do
  [ -n "$ONLY" ] && [ "$ONLY" != "$p" ] && continue
  case "$p" in
    A) path_A ;; B) path_B ;; C) path_C ;; D) path_D ;;
  esac
done

echo
echo "======================================================================"
printf ' summary: %d pass, %d warn, %d fail\n' "$PASS" "$WARN" "$FAIL"
echo "======================================================================"
if [ "$FAIL" -gt 0 ]; then
  echo " This machine cannot run at least one path as documented."
  echo " No Apple firmware, key or disk image lives in this repository:"
  echo " you fetch the IPSW yourself, from Apple's own servers."
else
  echo " No blocking problems found. Pick a path in README.md and read its"
  echo " section before you start - the setup effort differs by an order of"
  echo " magnitude (C: under an hour, A: an evening or two)."
fi
