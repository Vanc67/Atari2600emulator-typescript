package psx;

/**
 * A pre-decoded basic block: up to {@link #MAX_INSNS} instructions that are
 * executed until the first branch / jump / trap.
 *
 * Only two arrays are kept per block (the 16 bit dispatch key and the raw
 * instruction word) which keeps a fully populated 4 KiB code page below
 * 4 KiB of heap - important on CLDC devices with a 4-8 MiB heap.
 */
public final class CodeBlock {

    public static final int MAX_INSNS = 48;

    public final int startPc;
    public final int count;
    final short[] keys;
    final int[] raw;

    CodeBlock(int startPc, short[] keys, int[] raw, int count) {
        this.startPc = startPc;
        this.keys = keys;
        this.raw = raw;
        this.count = count;
    }

    /**
     * Executes the block.
     *
     * @return number of instructions executed (may be less than count when a
     *         branch was taken)
     */
    int exec(Cpu cpu) {
        int n = count;
        for (int i = 0; i < n; i++) {
            int r = cpu.execOp(keys[i], raw[i]);
            if (r != 0) return i + 1;
            if (cpu.codeDirty) return i + 1;   /* self modifying code */
        }
        return n;
    }
}
