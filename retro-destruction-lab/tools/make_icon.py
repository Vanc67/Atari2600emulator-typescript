#!/usr/bin/env python3
"""Generate the tiny indexed MIDlet icon without external image packages."""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parent.parent
PALETTE = [
    (11, 14, 23),
    (23, 31, 48),
    (82, 214, 211),
    (113, 131, 158),
    (213, 224, 232),
    (201, 53, 69),
    (240, 123, 50),
    (247, 201, 72),
]
ROWS = [
    "0000000000000000",
    "0222222222222220",
    "0200000300000020",
    "0200033330000020",
    "0200333340000020",
    "0200334000700020",
    "0200340076700020",
    "0200006777760020",
    "0200677555760020",
    "0200677555760020",
    "0200076777600020",
    "0200007667000020",
    "0200300700300020",
    "0203330003330020",
    "0222222222222220",
    "0000000000000000",
]


def chunk(kind: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)


def main() -> None:
    assert len(ROWS) == 16 and all(len(row) == 16 for row in ROWS)
    raw = b"".join(b"\0" + bytes(int(value) for value in row) for row in ROWS)
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 3, 0, 0, 0))
    png += chunk(b"PLTE", b"".join(bytes(color) for color in PALETTE))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    output = ROOT / "res" / "icon.png"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(png)
    print(f"Generated {output} ({len(png)} bytes, indexed 8-color PNG)")


if __name__ == "__main__":
    main()
