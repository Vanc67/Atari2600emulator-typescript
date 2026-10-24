"""Runs CoreRun from a dex file on the mini Dalvik interpreter.

Usage: run_core.py <classes.dex> <output.txt> [steps] [images...]

CoreRun keeps its frame budget on the command line because the interpreter is
slow; this passes the arguments through as a String[].
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dvmlite


def main(argv):
    if len(argv) < 3:
        sys.stderr.write(__doc__)
        return 2
    dex_path, out_path = argv[1], argv[2]
    rest = argv[3:]

    out = open(out_path, "w", buffering=1)

    def emit(line):
        out.write(line + "\n")
        out.flush()

    dex = dvmlite.Dex(open(dex_path, "rb").read())
    vm = dvmlite.Vm(dex, out=emit)
    args = vm.new_array("[Ljava/lang/String;", len(rest))
    for i, a in enumerate(rest):
        args.a[i] = a
    try:
        vm.invoke("LCoreRun;", "main", ("[Ljava/lang/String;",), "V", [args])
    except dvmlite.DexError as e:
        emit("DEXERROR %s" % e)
        emit("   in %s" % vm.last_method)
        return 1
    out.close()
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
