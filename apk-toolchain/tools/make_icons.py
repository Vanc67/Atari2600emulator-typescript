#!/usr/bin/env python3
"""
make_icons.py - renders the launcher icon PNGs with nothing but the stdlib.

No Pillow in the sandbox, so this is a small RGBA PNG writer plus a
procedural icon painter: a rounded-square tile (iOS 7+ silhouette) with a
vertical gradient, a stylised phone screen and a home button.

Usage: make_icons.py <output-res-dir> [source-png]
If source-png is given it is resampled into the buckets instead of being
painted procedurally (used when a nicer artwork file is available).
"""

import os
import struct
import sys
import zlib

BUCKETS = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def write_png(path, size, pixel_fn):
    """pixel_fn(x, y) -> (r, g, b, a)"""
    raw = bytearray()
    for y in range(size):
        raw.append(0)  # filter type 0 (None) for the whole scanline
        for x in range(size):
            r, g, b, a = pixel_fn(x, y)
            raw += bytes((int(r) & 255, int(g) & 255, int(b) & 255, int(a) & 255))

    def chunk(tag, data):
        return (
            struct.pack(">I", len(data))
            + tag
            + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    ihdr = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", ihdr)
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as fh:
        fh.write(png)


def paint(size):
    """Procedural icon: rounded tile + screen + home button."""
    radius = size * 0.2237  # iOS superellipse-ish corner radius
    cx = cy = size / 2.0
    # screen rect (the "device") in normalised units
    sx0, sy0 = size * 0.27, size * 0.13
    sx1, sy1 = size * 0.73, size * 0.87
    r_screen = size * 0.07
    home_cx, home_cy = size * 0.5, size * 0.795
    home_r = size * 0.045

    def inside_rounded_rect(x, y, x0, y0, x1, y1, r):
        if x < x0 or x > x1 or y < y0 or y > y1:
            return False
        # corner circles
        for ccx, ccy in ((x0 + r, y0 + r), (x1 - r, y0 + r), (x0 + r, y1 - r), (x1 - r, y1 - r)):
            if (x0 + r <= x <= x1 - r) or (y0 + r <= y <= y1 - r):
                continue
            if (x - ccx) ** 2 + (y - ccy) ** 2 > r * r:
                return False
        return True

    def pixel(x, y):
        fx, fy = x + 0.5, y + 0.5
        # --- tile background (rounded square) ---
        dx = max(abs(fx - cx) - (size / 2.0 - radius), 0.0)
        dy = max(abs(fy - cy) - (size / 2.0 - radius), 0.0)
        if (dx * dx + dy * dy) > radius * radius:
            return (0, 0, 0, 0)

        t = fy / size  # 0 at top, 1 at bottom
        # deep indigo -> warm violet, echoing an iOS 9 wallpaper
        r = 34 + (122 - 34) * t
        g = 40 + (58 - 40) * t
        b = 92 + (148 - 92) * t

        # subtle diagonal sheen
        sheen = max(0.0, 1.0 - abs((fx + fy) / size - 0.85) * 3.2) * 34
        r += sheen
        g += sheen
        b += sheen

        # --- screen ---
        if inside_rounded_rect(fx, fy, sx0, sy0, sx1, sy1, r_screen):
            st = (fy - sy0) / max(1e-6, (sy1 - sy0))
            r = 16 + (58 - 16) * st
            g = 20 + (36 - 20) * st
            b = 40 + (96 - 40) * st
            # home button ring
            hd = ((fx - home_cx) ** 2 + (fy - home_cy) ** 2) ** 0.5
            if hd < home_r:
                r, g, b = 226, 228, 234
            elif hd < home_r + size * 0.012:
                r, g, b = 150, 156, 172
            # status bar sliver
            elif fy < sy0 + size * 0.055:
                r, g, b = 236, 238, 244
            return (r, g, b, 255)

        return (min(r, 255), min(g, 255), min(b, 255), 255)

    return pixel


def main():
    out_root = sys.argv[1] if len(sys.argv) > 1 else "android/res"
    for bucket, size in BUCKETS.items():
        d = os.path.join(out_root, bucket)
        os.makedirs(d, exist_ok=True)
        write_png(os.path.join(d, "ic_launcher.png"), size, paint(size))
        print("  %-16s ic_launcher.png  %dx%d" % (bucket, size, size))


if __name__ == "__main__":
    main()
