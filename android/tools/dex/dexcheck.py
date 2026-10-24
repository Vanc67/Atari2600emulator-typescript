"""Structural check of a dex file: every instruction decodes and every
branch/switch target lands on an instruction boundary.

The translator emits absolute labels and the assembler turns them into
relative offsets, so a wrong offset shows up here as a target in the middle
of an instruction, even when no test ever reaches that branch.
"""
import sys

sys.path.insert(0, __file__.rsplit("/", 1)[0])
import dvmlite


def check(path):
    d = dvmlite.Dex(open(path, "rb").read())
    problems = []
    total = 0
    for name, c in sorted(d.classes.items()):
        for table in (c.direct, c.virtual):
            for m in table.values():
                if m.code is None:
                    continue
                body = d.code(m.code)
                insns = body["insns"]
                starts = set()
                targets = []
                pc = 0
                while pc < len(insns):
                    starts.add(pc)
                    w = insns[pc]
                    if w == 0x0100:   # packed: ident,size,first_key,targets[]
                        pc += 2 * insns[pc + 1] + 4
                        continue
                    if w == 0x0200:   # sparse: ident,size,keys[],targets[]
                        pc += 4 * insns[pc + 1] + 2
                        continue
                    if w == 0x0300:                  # fill-array-data payload
                        width = insns[pc + 1]
                        count = insns[pc + 2] | (insns[pc + 3] << 16)
                        pc += 4 + (count * width + 1) // 2
                        continue
                    try:
                        nm, ops, size = dvmlite.decode(insns, pc)
                    except dvmlite.DexError as e:
                        problems.append("%s: %s" % (m, e))
                        break
                    total += 1
                    if nm in ("goto", "goto/16", "goto/32"):
                        targets.append(pc + ops[0])
                    elif nm.startswith("if-"):
                        targets.append(pc + ops[-1])
                    elif nm in ("packed-switch", "sparse-switch"):
                        targets.extend(_switch_targets(insns, pc, nm, ops))
                    pc += size
                for t in targets:
                    if t not in starts and t < len(insns):
                        problems.append("%s: branch to %d, which is not an "
                                        "instruction" % (m, t))
                for (start, count, pairs, catch_all) in body["tries"]:
                    if start not in starts:
                        problems.append("%s: try start %d" % (m, start))
                    for (_t, addr) in pairs:
                        if addr not in starts:
                            problems.append("%s: handler %d" % (m, addr))
    return d, total, problems


def _s32(lo, hi):
    v = lo | (hi << 16)
    return v - 0x100000000 if v >= 0x80000000 else v


def _switch_targets(insns, pc, nm, ops):
    """Every target of a switch payload, as an absolute code-unit offset."""
    payload = pc + ops[1]
    count = insns[payload + 1]
    out = []
    if nm == "packed-switch":
        base = payload + 4
        for i in range(count):
            out.append(pc + _s32(insns[base + 2 * i], insns[base + 2 * i + 1]))
    else:
        base = payload + 2 + 2 * count
        for i in range(count):
            out.append(pc + _s32(insns[base + 2 * i], insns[base + 2 * i + 1]))
    return out


def main(argv):
    if len(argv) < 2:
        sys.stderr.write("usage: dexcheck.py <classes.dex> [...]\n")
        return 2
    bad = 0
    for path in argv[1:]:
        d, total, problems = check(path)
        print("%s: %d classes, %d instructions, %d problems"
              % (path, len(d.classes), total, len(problems)))
        for p in problems[:40]:
            print("   " + p)
        bad += len(problems)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
