package psx;

import java.io.IOException;

/**
 * A CD-ROM image the emulated drive can read sectors from.
 *
 * Two implementations ship with the emulator:
 *   {@link VirtualCD} - a file on the memory card (JSR-75, lazy rolling stream)
 *   {@link MemoryCD}  - a small image embedded in the JAR (the demo discs)
 */
public interface CdImage {

    boolean isOpen();

    /** Total number of 2048 byte user sectors. */
    int getSectorCount();

    /** Image layout constant from {@link VirtualCD}. */
    int getLayout();

    /** Reads the 2048 user data bytes of a sector. */
    void readSector(int lba, byte[] dst, int off) throws IOException;

    /** Reads a full 2352 byte raw sector (sync + header + data). */
    void readRawSector(int lba, byte[] dst) throws IOException;

    /** Reads an arbitrary byte range of user data starting at a sector. */
    int readUser(int lba, int offset, byte[] dst, int off, int len) throws IOException;

    /** Human readable source name for the OSD. */
    String getUrl();

    void close();
}
