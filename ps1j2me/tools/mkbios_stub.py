#!/usr/bin/env python3
"""Creates the 512 KiB placeholder BIOS shipped in the jar.

A real SCPH1001.BIN is copyrighted and cannot be redistributed. This stub
keeps the JAR layout identical (res/roms/scph1001.bin, 524288 bytes) and is
detected by the emulator, which then uses the built in HLE boot path
(SYSTEM.CNF -> PS-EX) instead of executing BIOS code.

Put your own dump at res/roms/scph1001.bin (or pass --bios to build.sh) and
the emulator will run the real BIOS instead.
"""
import os
import struct
import sys

SIZE = 512 * 1024

def build():
    img = bytearray(SIZE)
    # entry point: jump to the idle loop at 0xBFC00100
    struct.pack_into("<I", img, 0x00, 0x0AFF0040)   # j 0xBFC00100
    struct.pack_into("<I", img, 0x04, 0x00000000)   # nop
    marker = (b"HLE-STUB placeholder BIOS for PS1J2ME. Replace this file with "
              b"your own SCPH1001.BIN dump (512 KiB) to run the real BIOS.")
    img[0x40:0x40 + len(marker)] = marker
    # idle loop: beq zero, zero, -1
    struct.pack_into("<I", img, 0x100, 0x1000FFFF)
    # plausible ROM version strings
    img[0x200:0x200 + 30] = b"Sony Computer Entertainment Inc."
    return bytes(img[:SIZE])

def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "res/roms/scph1001.bin"
    d = os.path.dirname(out)
    if d and not os.path.isdir(d):
        os.makedirs(d)
    with open(out, "wb") as fh:
        fh.write(build())
    print("[bios] placeholder written to %s (%d bytes)" % (out, SIZE))

if __name__ == "__main__":
    main()
