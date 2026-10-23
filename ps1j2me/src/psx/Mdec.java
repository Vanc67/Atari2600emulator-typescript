package psx;

/**
 * MDEC stub. FMV decoding is not implemented in this build: the block keeps
 * the command/status handshake alive so that games fall back gracefully
 * instead of hanging on a busy MDEC.
 */
public final class Mdec {

    public int command;
    public int control;
    public int status;
    public Irq irq;

    public void reset() {
        command = 0;
        control = 0;
        status = 0x80040000;
    }

    public int read(int a) {
        if ((a & 0xF) == 0) return status;
        return 0xFFFFFFFF;
    }

    public void write(int a, int v) {
        if ((a & 0xF) == 0) {
            command = v;
            status = 0x80040000;
        } else {
            control = v;
            if ((v & 0x80000000) != 0) {
                command = 0;
                status = 0x80040000;
            }
        }
    }

    /** DMA input words are discarded. */
    public void writeDma(int word) { command = word; }

    public int readDma() { return 0; }
}
