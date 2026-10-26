#!/usr/bin/env python3
"""Converts the harness BMP frames to PNG (stdlib only)."""
import struct, sys, zlib, os, glob


def read_bmp(path):
    d = open(path, 'rb').read()
    if d[:2] != b'BM':
        raise ValueError('not a bmp')
    off = struct.unpack_from('<I', d, 10)[0]
    w = struct.unpack_from('<i', d, 18)[0]
    h = struct.unpack_from('<i', d, 22)[0]
    bpp = struct.unpack_from('<H', d, 28)[0]
    assert bpp == 24, bpp
    row = (w * 3 + 3) & ~3
    px = []
    for y in range(h):
        base = off + (h - 1 - y) * row
        line = bytearray(b'\x00')
        for x in range(w):
            b, g, r = d[base + x * 3: base + x * 3 + 3]
            line += bytes((r, g, b))
        px.append(bytes(line))
    return w, h, b''.join(px)


def write_png(path, w, h, raw):
    def chunk(t, data):
        c = struct.pack('>I', len(data)) + t + data
        return c + struct.pack('>I', zlib.crc32(t + data) & 0xFFFFFFFF)
    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(raw, 9))
    png += chunk(b'IEND', b'')
    open(path, 'wb').write(png)


def main():
    args = sys.argv[1:]
    if not args:
        print('usage: bmp2png.py file.bmp|dir ...')
        return 1
    for a in args:
        for p in (sorted(glob.glob(os.path.join(a, '*.bmp'))) if os.path.isdir(a) else [a]):
            w, h, raw = read_bmp(p)
            out = os.path.splitext(p)[0] + '.png'
            write_png(out, w, h, raw)
            print('%s -> %s (%dx%d)' % (p, out, w, h))
    return 0


if __name__ == '__main__':
    sys.exit(main())
