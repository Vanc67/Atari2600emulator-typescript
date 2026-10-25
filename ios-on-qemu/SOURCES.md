# Sources, licences, and who to blame for each fact

Everything asserted in `README.md` comes from one of the sources below. Where a
number appears (RAM, disk, boot time, supported firmware build), it is that
project's own documented figure, not an estimate.

## Emulators

| Project | What it is | Licence |
|---|---|---|
| [devos50/qemu-ios](https://github.com/devos50/qemu-ios) | QEMU fork that boots legacy Apple devices (iPod touch 1G/2G) from BootROM | GPL-2.0 (QEMU) |
| [samhenrigold/qemu-ios](https://github.com/samhenrigold/qemu-ios) | Active fork: adds `n18` (iPod touch 3G), `n88` (iPhone 3GS), `iPod-Touch-4G`, `iPhone-4` (n90ap), `ipad1`; touch, buttons, display, audio, Wi-Fi, USB via libimobiledevice, persistent storage, fake cellular, OpenGL ES through a host GL bridge. Powers [LightTouchMac](https://github.com/samhenrigold/LightTouchMac) | GPL-2.0 |
| [alephsecurity/xnu-qemu-arm64](https://github.com/alephsecurity/xnu-qemu-arm64) | QEMU fork that boots the iOS 12.1 XNU kernel of an iPhone 6s Plus (n66, A9/S8000) to a root shell | GPL-2.0 |
| [alephsecurity/xnu-qemu-arm64-tools](https://github.com/alephsecurity/xnu-qemu-arm64-tools) | Bootstrap scripts: ASN.1 kernel/devicetree/ramdisk decode, lzss decompress, trust cache creation | — |
| [ChefKissInc/Inferno](https://github.com/ChefKissInc/Inferno) | Apple Silicon device emulator (QEMU fork), machine `t8030` = iPhone 11 / A13 | GPL-3.0 / AGPL-3.0 |
| [TrungNguyen1909/qemu-t8030](https://github.com/TrungNguyen1909/qemu-t8030) | The t8030 machine model Inferno descends from | GPL-2.0+ |
| [lgrachov/ios-qemu](https://github.com/lgrachov/ios-qemu) | Unmodified iOS 16.7.16 iPhone X userland on QEMU `virt`, kernel rebuilt from Apple's open-source xnu-8796 | MIT (project code); kernel patches also APSL 2.0 |
| [XSLabs/qemu-sptm](https://github.com/XSLabs/qemu-sptm) | QEMU fork with SPTM/TXM, SPRR/GXF, Apple system registers, AIC v1-v3, SEP | — |
| [jprx/darwin-vm](https://github.com/jprx/darwin-vm) | Boots iOS/macOS (Darwin) to a **barebones root shell** on A14-A19. Its own README: *"This is not a full iPhone/Mac emulator. Don't expect the screen, wifi, bluetooth, graphics, GUI apps, or full springboard to work."* | — |
| [b1ackmarket/qemu-n90ap](https://github.com/b1ackmarket/qemu-n90ap) | iOS 4.3.1 XNU (xnu-1735, S5L8930/A4) boot bring-up, iPhone 3,1 `n90ap` and iPad 2 `k93ap` machine models, devicetree RAMDisk injector | — |

## Host-side recipes used as the basis for this directory

| Source | What was taken from it |
|---|---|
| [eugenesflanagin-spec/ios-emu-linux](https://github.com/eugenesflanagin-spec/ios-emu-linux) — `GUIDE.md`, `ios12-xnu-qemu/README.md`, `ios12-xnu-qemu/run-ios.sh`, `iphone11-qemuapplesilicon/run-iphone.sh` | The comparison table (what you see / touch / internet / persistence / boot time / setup effort), the hardware minimums, the Arch packaging route, the Linux-specific fixes, the troubleshooting table, and the exact QEMU command lines reproduced in `run-ios12-shell.sh` |
| [samhenrigold/qemu-ios `RUNNING.md`](https://github.com/samhenrigold/qemu-ios/blob/master/RUNNING.md) | Build flags, the `-M iPod-Touch,bootrom=,nand=,nor=` invocation, key bindings (H/P/=/-), USB device mode, the clean-shutdown requirement (HFS+ keeps catalog updates in memory, so killing QEMU loses the file) |
| [samhenrigold/qemu-ios `README.md`](https://github.com/samhenrigold/qemu-ios/blob/master/README.md) | The board table (machine name → device → SoC → board docs) reproduced in README §0, and the statement that no Apple firmware is in the repository |
| [lgrachov/ios-qemu `README.md`](https://github.com/lgrachov/ios-qemu/blob/master/README.md) | The what-works / what-does-not tables, the requirements (Apple Silicon + hvf, Homebrew qemu 11.0.2, ~40 GB), the four setup stages, and the "this is not Corellium" distinction |
| [alephsecurity.com — Running iOS in QEMU to an interactive bash shell (1) and (2)](https://alephsecurity.com/2019-06-17/xnu-qemu-arm64-1/) | The documented limitations quoted in README §1D: read-only ramdisk ≤2 GB, UART-only communication, *"No devices emulation: screen, touch, wifi, BT or anything else"*, single emulated CPU; and the `-m 6G` figure |
| [Black Hat EU 2019 whitepaper (Afek)](https://i.blackhat.com/eu-19/Wednesday/eu-19-Afek-Booting-The-iOS-Kernel-To-An-Interactive-Bash-Shell-On-QEMU-wp.pdf) | Same limitations, plus how the machine type, secure monitor, trust cache and launchd item were added |
| [worthdoingbadly.com — Almost booting an iOS kernel in QEMU](https://worthdoingbadly.com/xnuqemu/) and [part 2](https://worthdoingbadly.com/xnuqemu2/) | The honest framing: *"it doesn't emulate an iPhone at all, relying on only the parts common to all ARM devices. No drivers are loaded whatsoever"* |
| [devos50 — Emulating an iPod Touch 1G and iPhoneOS 1.0 using QEMU](https://devos50.github.io/blog/2022/ipod-touch-qemu/) | Where the S5L8720 reverse engineering is documented; also the 8900-encryption wall that stopped earlier attempts |
| [The Apple Wiki — QEMU-iOS](https://theapplewiki.com/wiki/QEMU-iOS), [Kernelcache](https://theapplewiki.com/wiki/Kernelcache), [Kernel](https://theapplewiki.com/wiki/Kernel), [IEmu](https://theapplewiki.com/wiki/IEmu) | Which firmware builds reach the home screen; the iOS 12+ monolithic stripped kernelcache format; the boot chain and boot-args behaviour; the note that *"Any Android app going by the name iEmu is a scam and should be avoided"* |
| [NixOS wiki — IOS Emulation](https://wiki.nixos.org/wiki/IOS_Emulation) | The QEMUAppleSilicon invocation, including why `sep-fw` and `sep-rom` have to be supplied on A12+ |
| [QEMU docs — Arm System emulator](https://www.qemu.org/docs/master/system/target-arm.html) | The upstream statement that settles the A5 question: *"If it is not listed [as a machine], then unfortunately your image will almost certainly not boot on QEMU."* |

## Tools referenced

[`ipsw`](https://github.com/blacktop/ipsw) (blacktop) — IPSW inspection, `img4
im4p extract`, kernel decryption. [`joker`](https://newosxbook.com/tools/joker.html)
(Jonathan Levin) — kernelcache unpacking and kext extraction.
[`ldid`](https://github.com/ProcursusTeam/ldid) — fake-signing.
[`libimobiledevice`](https://libimobiledevice.org) / `idevicerestore` — restore and
device communication. [`linux-apfs-rw`](https://github.com/linux-apfs/linux-apfs-rw),
[`apfs-fuse`](https://github.com/sgan81/apfs-fuse) — filesystem access from Linux.
[`lzfse`](https://github.com/lzfse/lzfse) — decompression of iOS 10+ payloads.
[apple-oss-distributions/xnu](https://github.com/apple-oss-distributions/xnu) and
[blacktop/darwin-xnu-build](https://github.com/blacktop/darwin-xnu-build) — the
open-source kernel that path B rebuilds.

## Licences of the files in this directory

`README.md`, `SOURCES.md`, `check-host.sh`, `ipsw-inspect.py`,
`tests/test_ipsw_inspect.py`, `run-ipod-touch-1g.sh`, `run-ios12-shell.sh`:
original work, MIT, same spirit as the repos they glue together.

They contain **no Apple firmware, keys, bootroms, kernelcaches or disk images**,
and no code copied from the GPL/AGPL emulators. The two launchers only assemble
command lines that the upstream projects themselves document; they modify no
emulator and ship no patch. Path A (Inferno) deliberately has no launcher: its
guide asks that setup automation not be published.

Screenshots of Apple software in the upstream repositories are included there for
documentation only. iOS, iPhone, iPod touch and Apple are trademarks of Apple
Inc.; none of these projects, and nothing in this directory, is affiliated with
or endorsed by Apple.

## Reproducing the sandbox findings

The "why not here" section of `README.md` is a measured result, not an opinion.
To reproduce it in any similar container:

```bash
./check-host.sh                       # prints PASS/WARN/FAIL per dependency
for p in glib-2.0 pixman-1 sdl2 gtk3 libslirp; do
  pkg-config --exists "$p" && echo "$p ok" || echo "$p MISSING"
done
for c in gcc make meson ninja pkg-config clang; do
  command -v "$c" >/dev/null && echo "$c ok" || echo "$c MISSING"
done
```

On the machine where this was written: `gcc`, `g++`, `make` present;
`pkg-config`, `meson`, `ninja`, `cmake`, `clang` absent; `/usr/include/glib-2.0`,
`/usr/include/pixman-1`, `/usr/include/SDL2`, `/usr/include/zlib.h` absent;
`apt-get` has no reachable package lists; 2 CPU threads. QEMU cannot be
configured, let alone built, under those conditions.
