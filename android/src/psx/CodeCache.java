package psx;

/**
 * Lazy page table of decoded {@link CodeBlock}s.
 *
 * <p>1 MiB of address space is covered by one page slot, the actual block
 * arrays are allocated only when code is first executed from that page.
 * Writing to RAM through the bus drops the affected page, so self modifying
 * code (and DMA loaded overlays) is handled without flushing everything.</p>
 */
public final class CodeCache {

    public static final int PAGE_SHIFT = 12;
    public static final int PAGE_SIZE = 1 << PAGE_SHIFT;
    static final int PAGE_MASK = 0x3FF; /* 1024 words per page */

    private static final int RAM_PAGES = 512;   /* 2 MiB                       */
    private static final int BIOS_PAGES = 128;  /* 512 KiB at 0x1FC00000       */

    private final Bus bus;
    private final CodeBlock[][] ramPages = new CodeBlock[RAM_PAGES][];
    private final CodeBlock[][] biosPages = new CodeBlock[BIOS_PAGES][];

    /** Blocks decoded for addresses that are not cacheable (MMIO, garbage). */
    private final CodeBlock[] temp = new CodeBlock[1];

    int pagesBuilt;
    int blocksBuilt;

    CodeCache(Bus bus) {
        this.bus = bus;
    }

    public void flush() {
        for (int i = 0; i < RAM_PAGES; i++) ramPages[i] = null;
        for (int i = 0; i < BIOS_PAGES; i++) biosPages[i] = null;
        temp[0] = null;
    }

    /** Drops the page that contains the given *physical* address. */
    public void invalidate(int physAddr) {
        int a = physAddr & 0x1FFFFFFF;
        if (a < 0x00800000) {
            int page = (a >>> PAGE_SHIFT) & (RAM_PAGES - 1);
            if (ramPages[page] != null) ramPages[page] = null;
        }
    }

    /** Drops every page touched by a block of host writes (DMA, GPU upload). */
    public void invalidateRange(int physAddr, int len) {
        int a = physAddr & 0x1FFFFFFF;
        if (a >= 0x00800000) return;
        int first = (a >>> PAGE_SHIFT) & (RAM_PAGES - 1);
        int last = ((a + len) >>> PAGE_SHIFT) & (RAM_PAGES - 1);
        if (last < first) {
            for (int i = first; i < RAM_PAGES; i++) ramPages[i] = null;
            for (int i = 0; i <= last; i++) ramPages[i] = null;
        } else {
            for (int i = first; i <= last; i++) ramPages[i] = null;
        }
    }

    CodeBlock blockAt(int pc) {
        int phys = pc & 0x1FFFFFFF;
        CodeBlock[][] pages;
        int pageIndex;
        if (phys < 0x00800000) {
            pages = ramPages;
            pageIndex = (phys >>> PAGE_SHIFT) & (RAM_PAGES - 1);
            return lookup(pages, pageIndex, pc, phys);
        }
        if (phys >= 0x1FC00000) {
            pages = biosPages;
            pageIndex = (phys >>> PAGE_SHIFT) & (BIOS_PAGES - 1);
            return lookup(pages, pageIndex, pc, phys);
        }
        /* Not code memory (scratchpad, MMIO): decode a throw-away block. */
        CodeBlock b = temp[0];
        if (b == null || b.startPc != pc) {
            b = decode(pc, phys);
            temp[0] = b;
        }
        return b;
    }

    private CodeBlock lookup(CodeBlock[][] pages, int pageIndex, int pc, int phys) {
        CodeBlock[] page = pages[pageIndex];
        if (page == null) {
            page = new CodeBlock[PAGE_MASK + 1];
            pages[pageIndex] = page;
            pagesBuilt++;
        }
        int slot = (phys >>> 2) & PAGE_MASK;
        CodeBlock b = page[slot];
        if (b == null) {
            b = decode(pc, phys);
            page[slot] = b;
            blocksBuilt++;
        }
        return b;
    }

    /** Builds a block starting at {@code pc}; it never crosses a page border. */
    private CodeBlock decode(int pc, int phys) {
        int pageEnd = (phys & ~(PAGE_SIZE - 1)) + PAGE_SIZE;
        short[] keys = new short[CodeBlock.MAX_INSNS];
        int[] raws = new int[CodeBlock.MAX_INSNS];
        int addr = pc;
        int n = 0;
        while (n < CodeBlock.MAX_INSNS && (addr & 0x1FFFFFFF) < pageEnd) {
            int raw = bus.read32(addr);
            int key = Cpu.decodeKey(raw);
            keys[n] = (short) key;
            raws[n] = raw;
            n++;
            addr += 4;
            if (Cpu.isTerminator(raw, key)) break;
        }
        if (n == 0) {
            keys[0] = 0;
            raws[0] = 0;
            n = 1;
        }
        return new CodeBlock(pc, keys, raws, n);
    }
}
