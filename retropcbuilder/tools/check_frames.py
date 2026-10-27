#!/usr/bin/env python3
"""Sanity check for rendered frames: sizes, non-empty, avatar palette hits."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import frames_to_gif as F

BG = (0x0B, 0x0E, 0x14)
AVATAR = {
    'skin': (0xF0, 0xB9, 0x8C), 'skinD': (0xC0, 0x8A, 0x62),
    'hair': (0x4A, 0x2C, 0x17), 'hairD': (0x2A, 0x1B, 0x10),
    'jacket': (0x2F, 0x8C, 0x6B), 'jacketD': (0x1E, 0x5A, 0x4A),
    'jeans': (0x39, 0x50, 0x7F), 'jeansD': (0x23, 0x2C, 0x4A),
    'shoe': (0xD8, 0xDE, 0xE8), 'shoeD': (0x6A, 0x62, 0x57),
}
AVATAR_COLORS = frozenset(AVATAR.values())


def load(path):
    w, h, rows = F.read_png(path)

    def at(x, y):
        r = rows[y]
        return (r[x * 3], r[x * 3 + 1], r[x * 3 + 2])

    nonbg = 0
    for y in range(h):
        r = rows[y]
        for x in range(w):
            if (r[x * 3], r[x * 3 + 1], r[x * 3 + 2]) != BG:
                nonbg += 1
    avatar = 0
    xs = []
    ys = []
    for y in range(h):
        r = rows[y]
        for x in range(w):
            c = (r[x * 3], r[x * 3 + 1], r[x * 3 + 2])
            if c in AVATAR_COLORS:
                avatar += 1
                xs.append(x)
                ys.append(y)
    bbox = ('-', '-', '-', '-')
    if avatar:
        bbox = (min(xs), min(ys), max(xs), max(ys))
    return w, h, nonbg, avatar, bbox


def main(args):
    dirs = args or ['docs/screens']
    files = []
    for d in dirs:
        files += sorted(os.listdir(d))
    files = [f for f in files if f.endswith('.png') and f != 'font.png']
    bad = 0
    for f in files:
        w, h, nonbg, avatar, bbox = load(os.path.join(dirs[0], f))
        flag = ''
        if nonbg < 200:
            flag = '  <-- too sparse'
            bad += 1
        print('%-26s %dx%d  ink=%5d  avatar=%4d  bbox=%s%s'
              % (f, w, h, nonbg, avatar, bbox, flag))
    print('checked %d frames, %d suspicious' % (len(files), bad))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
