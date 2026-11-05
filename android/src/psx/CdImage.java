package psx;

import java.io.IOException;

/**
 * A CD-ROM image the emulated drive can read sectors from.
 *
 * Two implementations ship with the emulator:
 *   {@code psx.droid.Disc} - an image on the SD card (java.io, sector cache)
 *   {@link MemoryCD}       - a small image embedded in the APK (the demos)
 */
public interface CdImage {

    /* Image layouts. Kept here so the whole core can name them without
       dragging in a platform specific file reader. */
    int LAYOUT_2048 = 0;
    int LAYOUT_2352_MODE1 = 1;
    int LAYOUT_2352_MODE2 = 2;
    int LAYOUT_2336_MODE2 = 3;

    boolean isOpen();

    /** Total number of 2048 byte user sectors. */
    int getSectorCount();

    /** Image layout, one of the LAYOUT_* constants above. */
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
