package rpc.harness;

import java.io.FileOutputStream;

/** Tiny 24 bit BMP writer (no image codec needed on the test machine). */
public final class Bmp {

    private Bmp() {
    }

    public static void write(String path, int[] argb, int w, int h) {
        try {
            int rowSize = (w * 3 + 3) & ~3;
            int dataSize = rowSize * h;
            int fileSize = 54 + dataSize;
            FileOutputStream out = new FileOutputStream(path);
            out.write('B');
            out.write('M');
            le32(out, fileSize);
            le32(out, 0);
            le32(out, 54);
            le32(out, 40);
            le32(out, w);
            le32(out, h);
            le16(out, 1);
            le16(out, 24);
            le32(out, 0);
            le32(out, dataSize);
            le32(out, 2835);
            le32(out, 2835);
            le32(out, 0);
            le32(out, 0);
            byte[] row = new byte[rowSize];
            for (int y = h - 1; y >= 0; y--) {
                int i = 0;
                for (int x = 0; x < w; x++) {
                    int p = argb[y * w + x];
                    row[i++] = (byte) (p & 0xFF);
                    row[i++] = (byte) ((p >> 8) & 0xFF);
                    row[i++] = (byte) ((p >> 16) & 0xFF);
                }
                while (i < rowSize) {
                    row[i++] = 0;
                }
                out.write(row, 0, rowSize);
            }
            out.close();
        } catch (Throwable t) {
            System.out.println("bmp write failed: " + t);
        }
    }

    private static void le32(FileOutputStream out, int v) throws java.io.IOException {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
        out.write((v >> 16) & 0xFF);
        out.write((v >> 24) & 0xFF);
    }

    private static void le16(FileOutputStream out, int v) throws java.io.IOException {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
    }
}
