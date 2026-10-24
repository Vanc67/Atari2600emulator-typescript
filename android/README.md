# PSX for Android 2.3.6

A PlayStation 1 emulator that runs on Gingerbread phones: MIPS R3000A
interpreter with a code cache, the GTE, a fixed-point software rasteriser, the
SPU, the CD-ROM controller with DMA, and an HLE BIOS so that no copyrighted
BIOS image is needed to boot a disc.

The APK is `android/PSX.apk` (built by `android/build.sh`, installed with
`adb install -r android/PSX.apk`).

## What it needs

* Android 2.3.6 (API 10) or newer, ARMv6 or better.  No native code, no
  OpenGL, no NDK: the whole emulator is Java compiled to Dalvik bytecode.
* About 3.5 MB of RAM for the machine itself (2 MB of main RAM, 1 MB of VRAM)
  plus the usual Dalvik overhead, so it fits a 512 MB device.

Minimum requirements are deliberately plain - the emulator is a single dex, no
resources, no permissions beyond reading the SD card.

## Where the code comes from

This is not the Java ME emulator in `ps1j2me/` wrapped in an Android shell.
That version targets CLDC 1.1 and is written against 2003-era APIs.  What was
reused here is the *core*: the CPU, bus, GTE, GPU, SPU, CD-ROM, DMA, timers,
pad and MDEC classes in `android/src/psx/` were copied from the J2ME tree and
then reworked for this platform - the disc loader reads files instead of the
JSR-75 FileConnection API, the audio sink feeds an `AudioTrack` instead of a
`javax.microedition.media` player, and the whole UI is new.

Everything else - the activity, the surface view, the file browser, the pad
mapping, the settings - is new code written for Android.

## Building

There is no Android SDK, no Gradle plugin and no `dx` on the build host, so
the APK is assembled by hand from pieces that are reachable:

```
tools/setup_toolchain.sh   # fetches apktool (smali assembler + aapt) and
                           # android.jar for API 10 into ~/.cache/psx-apk
./build.sh                 # ECJ java -> .class, jvm2smali -> .smali,
                           # dexlib2 -> classes.dex, aapt + v1 signature
```

`build.sh` produces `build/PSX.apk`.  The toolchain is only fetched once; after
that the build is offline and takes about four seconds.

The steps in detail:

| step | tool | what it does |
| --- | --- | --- |
| compile | Eclipse compiler (ECJ) | Java 1.6 source level, `-g:none`, against the real API 10 `android.jar` |
| translate | `tools/dex/jvm2smali.py` | JVM class files to Dalvik assembly, using a fixed register map (scratch, locals, operand stack, parameters) |
| assemble | dexlib2 (inside apktool) | smali to `classes.dex`, API level 10 |
| package | `aapt` + `tools/apkpack.py` | manifest, assets, dex; then a hand rolled v1 (JAR) signature |
| check | `tools/ApkVerify.java` | re-reads the APK and verifies every manifest digest |

## Verifying it

`./tools/verify.sh` runs three checks and is what the release builds are
validated with:

1. **Structure** - `tools/dex/dexcheck.py` walks every method in the shipped
   dex: all 28 700 instructions decode, and every branch, switch target and
   exception handler lands on an instruction boundary.
2. **Language** - `tools/dex/selftest/Probe.java` exercises the features the
   translator must support (dense and sparse switches, arrays of every type,
   `try`/`catch`/`finally`, long arithmetic, casts, interfaces, string
   building).  It runs on a JVM and on the generated dex; the output has to be
   identical.
3. **Core** - `tools/dex/selftest/CoreRun.java` boots a demo disc through the
   HLE BIOS and single steps the MIPS core, printing a fingerprint of the CPU,
   GPU VRAM and frame buffer.  Again JVM versus dex, byte for byte.

Number 3 is the important one: it runs the *actual* emulator, so a wrong
register in the translator shows up as a different VRAM hash within a few
thousand instructions rather than as a black screen on someone's phone.

The dex runs on `tools/dex/dvmlite.py`, a small Dalvik interpreter written for
this purpose (there is no emulator on the build host).  It implements the
subset of Dalvik that our code generator emits, plus the standard library
calls the core uses.

## Using it

* The file browser lists `/sdcard` for `.iso`, `.bin` and `.cue` files.
* Start button opens the menu; D-pad and the face buttons are on screen, and
  the volume keys map to L2/R2.  Hardware keys work too (arrows, Enter, A/S/D
  and so on - see `Keys.java`).
* Discs are streamed: only a rolling window of sectors is kept in memory, so
  a 700 MB image never gets loaded whole.

## Layout

```
android/
  build.sh                 the whole build
  AndroidManifest.xml      psx.droid, minSdk/targetSdk 10
  assets/demos/*.iso       seven demo discs, also used by the verifier
  src/psx/                 emulator core (CPU, bus, GPU, SPU, CD-ROM, ...)
  src/psx/droid/           Android front end (activity, view, engine, disc I/O)
  tools/dex/jvm2smali.py   JVM bytecode -> Dalvik assembly
  tools/dex/dvmlite.py     Dalvik interpreter used by the verifier
  tools/dex/dexcheck.py    structural check of a dex file
  tools/dex/selftest/      the two verification programs
  tools/setup_toolchain.sh fetches ECJ, apktool and android.jar
  tools/verify.sh          the three verification passes
```
