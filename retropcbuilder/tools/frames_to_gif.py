#!/usr/bin/env python3
"""Builds docs/screens/tour.gif from the rendered PNG frames (stdlib only).

A GIF is the easiest way to show a reviewer the actual screen flow without
making them open a dozen BMPs.
"""
import glob
import os
import struct
import zlib
import sys


def read_png(path):
    data = open(path, 'rb').read()
    assert data[:8] == b'\x89PNG\r\n\x1a\n', path
    pos = 8
    w = h = None
    idat = b''
    while pos < len(data):
        length = struct.unpack_from('>I', data, pos)[0]
        kind = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b'IHDR':
            w, h, depth, color = struct.unpack_from('>IIBB', body, 0)
            assert depth == 8 and color == 2, (depth, color)
        elif kind == b'IDAT':
            idat += body
        elif kind == b'IEND':
            break
    raw = zlib.decompress(idat)
    stride = w * 3 + 1
    rows = []
    prev = bytearray(w * 3)
    for y in range(h):
        off = y * stride
        f = raw[off]
        line = bytearray(raw[off + 1:off + 1 + w * 3])
        if f == 1:
            for i in range(3, len(line)):
                line[i] = (line[i] + line[i - 3]) & 0xFF
        elif f == 2:
            for i in range(len(line)):
                line[i] = (line[i] + prev[i]) & 0xFF
        elif f == 3:
            for i in range(len(line)):
                a = line[i - 3] if i >= 3 else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif f == 4:
            for i in range(len(line)):
                a = line[i - 3] if i >= 3 else 0
                b = prev[i]
                c = prev[i - 3] if i >= 3 else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 0xFF
        rows.append(bytes(line))
        prev = line
    return w, h, rows


def quantize(pixels, w, h):
    """Palette built from the frame's own colours.

    The game draws with a small fixed palette, so the exact set of colours in
    a frame is usually well under 256 entries; in that case the image is
    reproduced losslessly.  Only busier frames fall back to a 3-3-2 cube.
    """
    counts = {}
    for y in range(h):
        row = pixels[y]
        for x in range(w):
            c = (row[x * 3], row[x * 3 + 1], row[x * 3 + 2])
            counts[c] = counts.get(c, 0) + 1
    if len(counts) <= 256:
        palette = sorted(counts.keys(), key=lambda c: -counts[c])
        index = {}
        for i, c in enumerate(palette):
            index[c] = i
        out = bytearray()
        for y in range(h):
            row = pixels[y]
            for x in range(w):
                out.append(index[(row[x * 3], row[x * 3 + 1], row[x * 3 + 2])])
    else:
        palette = []
        seen = {}
        for y in range(h):
            row = pixels[y]
            for x in range(w):
                r, g, b = row[x * 3], row[x * 3 + 1], row[x * 3 + 2]
                key = (r >> 3, g >> 3, b >> 2)
                if key not in seen and len(palette) < 256:
                    seen[key] = len(palette)
                    palette.append(((r >> 3) << 3 | 4, (g >> 3) << 3 | 4,
                                    (b >> 2) << 2 | 2))
        for y in range(h):
            row = pixels[y]
            for x in range(w):
                r, g, b = row[x * 3], row[x * 3 + 1], row[x * 3 + 2]
                key = (r >> 3, g >> 3, b >> 2)
                if key not in seen:
                    best = min(palette, key=lambda p: (p[0] - r) ** 2
                               + (p[1] - g) ** 2 + (p[2] - b) ** 2)
                    seen[key] = palette.index(best)
        out = bytearray()
        for y in range(h):
            row = pixels[y]
            for x in range(w):
                r, g, b = row[x * 3], row[x * 3 + 1], row[x * 3 + 2]
                out.append(seen[(r >> 3, g >> 3, b >> 2)])
    while len(palette) < 256:
        palette.append((0, 0, 0))
    flat = b''.join(bytes(c) for c in palette)
    return bytes(out), flat


def lzw_encode(indices, min_code_size=8):
    """GIF LZW compression (variable width codes, little-endian packing)."""
    clear = 1 << min_code_size
    end = clear + 1
    code_size = min_code_size + 1
    next_code = end + 1
    table = {}
    out = bytearray()
    bitbuf = 0
    bitcount = 0

    def emit(code):
        nonlocal bitbuf, bitcount
        bitbuf |= code << bitcount
        bitcount += code_size
        while bitcount >= 8:
            out.append(bitbuf & 0xFF)
            bitbuf >>= 8
            bitcount -= 8

    emit(clear)
    prefix = indices[0]
    for b in indices[1:]:
        key = (prefix << 8) | b
        if key in table:
            prefix = table[key]
            continue
        emit(prefix)
        if next_code < 4096:
            table[key] = next_code
            next_code += 1
            if next_code > (1 << code_size) and code_size < 12:
                code_size += 1
        else:
            emit(clear)
            table = {}
            code_size = min_code_size + 1
            next_code = end + 1
        prefix = b
    emit(prefix)
    emit(end)
    if bitcount > 0:
        out.append(bitbuf & 0xFF)
    return bytes(out)


def write_sub_blocks(out, data):
    pos = 0
    while pos < len(data):
        chunk = data[pos:pos + 255]
        out += bytes((len(chunk),)) + chunk
        pos += 255
    out += b'\x00'

def write_gif(path, frames, w, h, delay_cs=120):
    pal = None
    packed = []
    for (fw, fh, rows) in frames:
        assert (fw, fh) == (w, h)
        data, pal = quantize(rows, w, h)
        packed.append((data, pal))

    out = bytearray(b'GIF89a')
    out += struct.pack('<HH', w, h)
    out += bytes((0x70, 0, 0))          # no global colour table
    # loop forever
    out += b'\x21\xFF\x0BNETSCAPE2.0\x03\x01\x00\x00\x00'
    for data, local_pal in packed:
        out += b'\x21\xF9\x04\x04' + struct.pack('<H', delay_cs) + b'\x00\x00'
        # local colour table: 256 entries, present
        out += b'\x2C' + struct.pack('<HHHH', 0, 0, w, h) + bytes((0x87,))
        out += local_pal[:768].ljust(768, b'\x00')
        out += bytes((8,))
        write_sub_blocks(out, lzw_encode(data))
    out += b'\x3B'
    open(path, 'wb').write(bytes(out))


def main():
    pattern = sys.argv[1] if len(sys.argv) > 1 else 'docs/screens/*.png'
    out_path = sys.argv[2] if len(sys.argv) > 2 else 'docs/screens/tour.gif'
    files = sorted(p for p in glob.glob(pattern)
                   if not p.endswith('tour.gif') and 'font' not in p)
    frames = [read_png(p) for p in files]
    if not frames:
        print('no frames')
        return 1
    # keep only frames of the phone resolution (the font sheet is wider)
    w, h = frames[0][0], frames[0][1]
    frames = [f for f in frames if (f[0], f[1]) == (w, h)]
    files = [p for p, f in zip(files, frames)]
    print('frames: ' + ', '.join(os.path.basename(p) for p in files))
    write_gif(out_path, frames, w, h)
    print('%s <- %d frames, %dx%d, %.0f KiB' % (out_path, len(frames), w, h,
                                               os.path.getsize(out_path) / 1024))
    return 0


if __name__ == '__main__':
    sys.exit(main())
